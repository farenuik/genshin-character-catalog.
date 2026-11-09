package com.myproject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;

/** Downloads character portraits once and keeps them in a local directory. */
public final class FilePortraitCache implements PortraitCache {
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;

    private final HttpClient httpClient;
    private final Path directory;

    public FilePortraitCache(Path directory) {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build(), directory);
    }

    public FilePortraitCache(HttpClient httpClient, Path directory) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.directory = Objects.requireNonNull(directory, "directory").toAbsolutePath();
    }

    public static FilePortraitCache defaultCache() {
        return new FilePortraitCache(Path.of(
                System.getProperty("user.home"), ".genshin-cli", "portraits"));
    }

    @Override
    public synchronized Path cache(String imageUrl) throws IOException {
        URI imageUri = parseImageUri(imageUrl);
        String fileName = sha256(imageUrl);
        for (String extension : new String[] {"png", "jpg", "gif", "bmp", "webp", "img"}) {
            Path cachedFile = directory.resolve(fileName + "." + extension);
            if (Files.isRegularFile(cachedFile)) {
                long cachedSize = Files.size(cachedFile);
                if (cachedSize > 0 && cachedSize <= MAX_IMAGE_SIZE) {
                    if ("img".equals(extension)) {
                        String detectedExtension = detectExtension(cachedFile);
                        if (detectedExtension != null) {
                            Path renamedFile = directory.resolve(fileName + "." + detectedExtension);
                            try {
                                return Files.move(cachedFile, renamedFile, StandardCopyOption.REPLACE_EXISTING);
                            } catch (IOException exception) {
                                throw new IOException("Could not rename cached portrait to ." + detectedExtension,
                                        exception);
                            }
                        }
                    }
                    return cachedFile;
                }
                Files.delete(cachedFile);
            }
        }

        Files.createDirectories(directory);
        Path temporaryFile = Files.createTempFile(directory, "portrait-", ".tmp");
        try {
            HttpResponse<InputStream> response = sendRequest(imageUri);
            try (InputStream body = response.body()) {
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new IOException("Portrait server returned HTTP " + response.statusCode());
                }
                String contentType = response.headers()
                        .firstValue("Content-Type")
                        .orElse("")
                        .split(";", 2)[0]
                        .trim()
                        .toLowerCase(Locale.ROOT);
                if (!contentType.startsWith("image/")) {
                    throw new IOException("Portrait server returned a non-image response");
                }
                long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1);
                if (contentLength > MAX_IMAGE_SIZE) {
                    throw new IOException("Portrait image exceeds the 10 MiB size limit");
                }
                long imageSize = copyImage(body, temporaryFile);
                if (imageSize == 0) {
                    throw new IOException("Portrait image must not be empty");
                }
                Path imageFile = directory.resolve(fileName + "." + extensionForContentType(contentType));
                try {
                    Files.move(temporaryFile, imageFile,
                            StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException exception) {
                    Files.move(temporaryFile, imageFile, StandardCopyOption.REPLACE_EXISTING);
                }
                return imageFile;
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private static String extensionForContentType(String contentType) {
        return switch (contentType) {
            case "image/png" -> "png";
            case "image/jpeg", "image/jpg" -> "jpg";
            case "image/gif" -> "gif";
            case "image/bmp", "image/x-ms-bmp" -> "bmp";
            case "image/webp" -> "webp";
            default -> "img";
        };
    }

    private static String detectExtension(Path imageFile) throws IOException {
        byte[] signature = new byte[8];
        try (InputStream input = Files.newInputStream(imageFile)) {
            int bytesRead = input.read(signature);
            if (bytesRead >= 8
                    && (signature[0] & 0xff) == 0x89
                    && signature[1] == 'P' && signature[2] == 'N' && signature[3] == 'G'
                    && signature[4] == 0x0d && signature[5] == 0x0a
                    && signature[6] == 0x1a && signature[7] == 0x0a) {
                return "png";
            }
            if (bytesRead >= 3 && (signature[0] & 0xff) == 0xff
                    && (signature[1] & 0xff) == 0xd8 && (signature[2] & 0xff) == 0xff) {
                return "jpg";
            }
            if (bytesRead >= 6 && signature[0] == 'G' && signature[1] == 'I'
                    && signature[2] == 'F' && signature[3] == '8') {
                return "gif";
            }
            if (bytesRead >= 2 && signature[0] == 'B' && signature[1] == 'M') {
                return "bmp";
            }
            return null;
        }
    }

    private static long copyImage(InputStream source, Path destination) throws IOException {
        try (OutputStream output = Files.newOutputStream(destination)) {
            byte[] buffer = new byte[8192];
            long totalBytes = 0;
            int bytesRead;
            while ((bytesRead = source.read(buffer)) != -1) {
                if (totalBytes + bytesRead > MAX_IMAGE_SIZE) {
                    throw new IOException("Portrait image exceeds the 10 MiB size limit");
                }
                output.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
            }
            return totalBytes;
        }
    }

    private HttpResponse<InputStream> sendRequest(URI imageUri) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(imageUri)
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "image/*")
                .GET()
                .build();
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Portrait download was interrupted", exception);
        }
    }

    private static URI parseImageUri(String imageUrl) throws IOException {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IOException("Portrait URL must not be blank");
        }
        try {
            URI uri = URI.create(imageUrl);
            String scheme = uri.getScheme();
            if (uri.getHost() == null || !("http".equalsIgnoreCase(scheme)
                    || "https".equalsIgnoreCase(scheme))) {
                throw new IOException("Portrait URL must be an absolute HTTP(S) URL");
            }
            return uri;
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid portrait URL", exception);
        }
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}

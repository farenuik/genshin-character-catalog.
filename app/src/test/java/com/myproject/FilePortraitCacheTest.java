package com.myproject;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FilePortraitCacheTest {
    private static final byte[] PORTRAIT_BYTES = new byte[] {
        (byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a
    };

    @TempDir
    Path temporaryDirectory;

    private HttpServer server;
    private AtomicInteger requestCount;
    private String imageUrl;

    @BeforeEach
    void startServer() throws IOException {
        requestCount = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/portrait.png", exchange -> {
            requestCount.incrementAndGet();
            exchange.getResponseHeaders().set("Content-Type", "image/png");
            exchange.sendResponseHeaders(200, PORTRAIT_BYTES.length);
            try (var output = exchange.getResponseBody()) {
                output.write(PORTRAIT_BYTES);
            }
        });
        server.createContext("/not-an-image", exchange -> {
            byte[] body = "not an image".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
        imageUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/portrait.png";
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void downloadsPortraitOnceAndReusesLocalFile() throws IOException {
        FilePortraitCache cache = new FilePortraitCache(temporaryDirectory.resolve("portraits"));

        Path firstDownload = cache.cache(imageUrl);
        Path cachedDownload = cache.cache(imageUrl);

        assertEquals(firstDownload, cachedDownload);
        assertTrue(Files.isRegularFile(firstDownload));
        assertTrue(firstDownload.getFileName().toString().endsWith(".png"));
        assertArrayEquals(PORTRAIT_BYTES, Files.readAllBytes(firstDownload));
        assertEquals(1, requestCount.get());
    }

    @Test
    void renamesLegacyImageCacheToItsImageExtension() throws IOException {
        FilePortraitCache cache = new FilePortraitCache(temporaryDirectory.resolve("portraits"));
        Path previousCacheFile = cache.cache(imageUrl);
        Path legacyCacheFile = previousCacheFile.resolveSibling(
                previousCacheFile.getFileName().toString().replaceFirst("\\.png$", ".img"));
        Files.move(previousCacheFile, legacyCacheFile);

        Path migratedCacheFile = cache.cache(imageUrl);

        assertTrue(migratedCacheFile.getFileName().toString().endsWith(".png"));
        assertTrue(Files.isRegularFile(migratedCacheFile));
        assertTrue(Files.notExists(legacyCacheFile));
        assertArrayEquals(PORTRAIT_BYTES, Files.readAllBytes(migratedCacheFile));
        assertEquals(1, requestCount.get());
    }

    @Test
    void rejectsNonImageResponsesAndLeavesNoCachedFile() throws IOException {
        FilePortraitCache cache = new FilePortraitCache(temporaryDirectory.resolve("portraits"));

        assertThrows(IOException.class, () -> cache.cache(
                "http://127.0.0.1:" + server.getAddress().getPort() + "/not-an-image"));

        try (var files = Files.list(temporaryDirectory.resolve("portraits"))) {
            assertEquals(0, files.count());
        }
    }
}

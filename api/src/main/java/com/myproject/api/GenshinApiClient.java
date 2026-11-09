package com.myproject.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myproject.game.Character;
import com.myproject.game.CharacterProfile;
import com.myproject.game.CharacterSkill;
import com.myproject.game.Equipment;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class GenshinApiClient implements GenshinDataSource {
    private static final String DEFAULT_BASE_URL = "https://genshin-db-api.vercel.app/api";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public GenshinApiClient() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(), DEFAULT_BASE_URL);
    }

    public GenshinApiClient(HttpClient httpClient, String baseUrl) {
        this.httpClient = java.util.Objects.requireNonNull(httpClient, "httpClient");
        this.objectMapper = new ObjectMapper();
        this.baseUrl = java.util.Objects.requireNonNull(baseUrl, "baseUrl").replaceAll("/+$", "");
    }

    @Override
    public List<String> listCharacterIds() throws IOException {
        return getStringList("/v5/characters?query=names&matchCategories=true");
    }

    @Override
    public List<String> listWeaponIds() throws IOException {
        return getStringList("/v5/weapons?query=names&matchCategories=true");
    }

    @Override
    public Character getCharacter(String id) throws IOException {
        CharacterCatalogEntry entry = getCharacterCatalogEntry(id);
        CharacterProfile profile = new CharacterProfile(
                entry.id(),
                entry.description(),
                entry.rarity(),
                entry.element(),
                entry.weaponType(),
                getCharacterSkills(id),
                entry.imageUrl());
        int health = randomStat(800 + entry.rarity() * 200, 1000 + entry.rarity() * 250);
        int armor = randomStat(40 + entry.rarity() * 20, 70 + entry.rarity() * 25);
        int attack = randomStat(60 + entry.rarity() * 30, 105 + entry.rarity() * 35);
        return new Character(entry.name(), health, armor, attack, profile);
    }

    @Override
    public CharacterCatalogEntry getCharacterCatalogEntry(String id) throws IOException {
        JsonNode data = getJson("/v5/characters?query=" + encode(id));
        if (data == null || data.isNull()) {
            throw new IOException("Genshin data API returned no character for query: " + id);
        }
        JsonNode images = data.path("images");
        String imageUrl = text(images, "hoyowiki_icon", text(images, "mihoyo_icon", ""));
        return new CharacterCatalogEntry(
                id,
                text(data, "name", id),
                text(data, "description", ""),
                integer(data, "rarity", 4),
                text(data, "elementText", ""),
                text(data, "weaponText", ""),
                imageUrl,
                List.of());
    }

    @Override
    public Equipment getWeapon(String id) throws IOException {
        JsonNode data = getJson("/v5/weapons?query=" + encode(id));
        if (data == null || data.isNull()) {
            throw new IOException("Genshin data API returned no weapon for query: " + id);
        }
        String name = text(data, "name", id);
        String description = text(data.path("r1"), "description",
                text(data, "effectName", text(data, "description", "")));
        String weaponType = text(data, "weaponText", "");
        return new Equipment(name, (int) Math.round(decimal(data, "baseAtkValue", 0)), 0, null,
                description, integer(data, "rarity", 0), weaponType);
    }

    private List<CharacterSkill> getCharacterSkills(String id) throws IOException {
        JsonNode talents = getJson("/v5/talents?query=" + encode(id));
        List<CharacterSkill> skills = new ArrayList<>();
        appendNumberedSkills(skills, talents, "combat", 3, "Combat");
        appendNumberedSkills(skills, talents, "passive", 3, "Passive");
        appendNumberedSkills(skills, talents, "constellation", 6, "Constellation");
        return List.copyOf(skills);
    }

    private static void appendNumberedSkills(
            List<CharacterSkill> result, JsonNode talents, String prefix, int count, String type) {
        for (int index = 1; index <= count; index++) {
            JsonNode talent = talents.path(prefix + index);
            String name = text(talent, "name", "");
            String description = text(talent, "description", text(talent, "descriptionRaw", ""));
            if (!name.isBlank() || !description.isBlank()) {
                result.add(new CharacterSkill(name, description, type));
            }
        }
    }

    private List<String> getStringList(String path) throws IOException {
        JsonNode response = getJson(path);
        if (!response.isArray()) throw new IOException("Unexpected catalog response from " + path);
        List<String> ids = new ArrayList<>();
        response.forEach(value -> {
            if (value.isTextual()) ids.add(value.asText());
        });
        return List.copyOf(ids);
    }

    private JsonNode getJson(String path) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Request to Genshin data API was interrupted", exception);
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Genshin data API returned HTTP " + response.statusCode() + " for " + path);
        }
        JsonNode result = objectMapper.readTree(response.body());
        if (result == null) {
            throw new IOException("Genshin data API returned an empty response for " + path);
        }
        return result;
    }

    private static String text(JsonNode node, String field, String fallback) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? fallback : value.asText(fallback);
    }

    private static int integer(JsonNode node, String field, int fallback) {
        JsonNode value = node.path(field);
        if (value.canConvertToInt()) return value.asInt();
        try {
            return Integer.parseInt(value.asText());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static double decimal(JsonNode node, String field, double fallback) {
        JsonNode value = node.path(field);
        if (value.isNumber()) return value.asDouble();
        try {
            return Double.parseDouble(value.asText());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static int randomStat(int minimum, int maximum) {
        return ThreadLocalRandom.current().nextInt(minimum, maximum + 1);
    }

    private static String encode(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("API id must not be blank");
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
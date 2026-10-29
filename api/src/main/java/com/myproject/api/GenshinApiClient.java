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
    private static final String DEFAULT_BASE_URL = "https://genshin.jmp.blue";

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
        return getStringList("/characters");
    }

    @Override
    public List<String> listWeaponIds() throws IOException {
        return getStringList("/weapons");
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
                entry.skills(),
                entry.imageUrl());
        int health = randomStat(800 + entry.rarity() * 200, 1000 + entry.rarity() * 250);
        int armor = randomStat(40 + entry.rarity() * 20, 70 + entry.rarity() * 25);
        int attack = randomStat(60 + entry.rarity() * 30, 105 + entry.rarity() * 35);
        return new Character(entry.name(), health, armor, attack, profile);
    }

    @Override
    public CharacterCatalogEntry getCharacterCatalogEntry(String id) throws IOException {
        String encodedId = encode(id);
        JsonNode data = getJson("/characters/" + encodedId + "?lang=en");
        List<CharacterSkill> skills = new ArrayList<>();
        appendSkills(skills, data.path("skillTalents"), "Combat");
        appendSkills(skills, data.path("passiveTalents"), "Passive");
        appendSkills(skills, data.path("constellations"), "Constellation");
        return new CharacterCatalogEntry(
                id,
                text(data, "name", id),
                text(data, "description", ""),
                integer(data, "rarity", 4),
                text(data, "vision", ""),
                text(data, "weapon", ""),
                baseUrl + "/characters/" + encodedId + "/icon",
                skills);
    }

    @Override
    public Equipment getWeapon(String id) throws IOException {
        JsonNode data = getJson("/weapons/" + encode(id) + "?lang=en");
        String name = text(data, "name", id);
        String description = text(data, "passiveDesc", text(data, "description", ""));
        String weaponType = text(data, "type", "");
        return new Equipment(name, integer(data, "baseAttack", 0), 0, null,
                description, integer(data, "rarity", 0), weaponType);
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
            throw new IOException("Request to Genshin API was interrupted", exception);
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Genshin API returned HTTP " + response.statusCode() + " for " + path);
        }
        return objectMapper.readTree(response.body());
    }

    private static void appendSkills(List<CharacterSkill> result, JsonNode skills, String type) {
        if (!skills.isArray()) return;
        for (JsonNode skill : skills) {
            String name = text(skill, "name", "");
            String description = text(skill, "description", "");
            if (!name.isBlank() || !description.isBlank()) {
                result.add(new CharacterSkill(name, description, type));
            }
        }
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

    private static int randomStat(int minimum, int maximum) {
        return ThreadLocalRandom.current().nextInt(minimum, maximum + 1);
    }

    private static String encode(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("API id must not be blank");
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
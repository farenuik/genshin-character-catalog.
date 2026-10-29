package com.myproject.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.myproject.game.Character;
import com.myproject.game.Equipment;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GenshinApiClientTest {
    private HttpServer server;
    private GenshinApiClient client;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/characters", exchange -> respond(exchange, switch (exchange.getRequestURI().getPath()) {
            case "/characters" -> "[\"amber\",\"lisa\"]";
            default -> """
                    {"name":"Amber","rarity":4,"vision":"Pyro","weapon":"Bow",
                     "description":"A reliable outrider.",
                     "skillTalents":[{"name":"Explosive Puppet","description":"Deploys Baron Bunny."}],
                     "passiveTalents":[{"name":"Gliding Champion","description":"Reduces gliding stamina."}]}
                    """;
        }));
        server.createContext("/weapons", exchange -> respond(exchange, switch (exchange.getRequestURI().getPath()) {
            case "/weapons" -> "[\"favonius-warbow\"]";
            default -> """
                    {"name":"Favonius Warbow","type":"Bow","rarity":4,
                     "baseAttack":41,"passiveDesc":"CRIT hits generate energy."}
                    """;
        }));
        server.start();
        client = new GenshinApiClient(java.net.http.HttpClient.newHttpClient(),
                "http://127.0.0.1:" + server.getAddress().getPort());
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void listsEntitiesAndMapsCharacterAndWeaponData() throws IOException {
        assertEquals(List.of("amber", "lisa"), client.listCharacterIds());
        assertEquals(List.of("favonius-warbow"), client.listWeaponIds());

        Character character = client.getCharacter("amber");
        assertEquals("Amber", character.getName());
        assertEquals("Pyro", character.getProfile().element());
        assertEquals("Bow", character.getProfile().weaponType());
        assertEquals(2, character.getProfile().skills().size());
        assertEquals("http://127.0.0.1:" + server.getAddress().getPort() + "/characters/amber/icon",
                character.getProfile().imageUrl());
        assertTrue(character.getHealth() >= 1600 && character.getHealth() <= 2000);
        assertTrue(character.getArmor() >= 120 && character.getArmor() <= 170);
        assertTrue(character.getAttack() >= 180 && character.getAttack() <= 245);

        Equipment weapon = client.getWeapon("favonius-warbow");
        assertEquals("Favonius Warbow", weapon.getName());
        assertEquals(41, weapon.getAttackBonus());
        assertEquals("Bow", weapon.getWeaponType());
        assertTrue(weapon.getDescription().contains("CRIT"));
    }

    @Test
    void parsesApiJsonIntoImmutableCatalogEntryWithoutInventingCombatStats() throws IOException {
        CharacterCatalogEntry entry = client.getCharacterCatalogEntry("amber");

        assertEquals("amber", entry.id());
        assertEquals("Amber", entry.name());
        assertEquals("A reliable outrider.", entry.description());
        assertEquals(4, entry.rarity());
        assertEquals("Pyro", entry.element());
        assertEquals("Bow", entry.weaponType());
        assertEquals("http://127.0.0.1:" + server.getAddress().getPort() + "/characters/amber/icon",
                entry.imageUrl());
        assertEquals(List.of("Combat", "Passive"),
                entry.skills().stream().map(skill -> skill.type()).toList());
        assertFalse(entry.skills().isEmpty());
        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> entry.skills().add(entry.skills().get(0)));
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, String body) throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(200, response.length);
        try (var output = exchange.getResponseBody()) {
            output.write(response);
        }
    }
}
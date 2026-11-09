package com.myproject.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.myproject.game.Character;
import com.myproject.game.CharacterSkill;
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
        server.createContext("/v5/characters", exchange -> respond(exchange,
                exchange.getRequestURI().getRawQuery().contains("query=names")
                        ? "[\"Amber\",\"Lisa\"]"
                        : """
                                {"name":"Amber","rarity":4,"elementText":"Pyro","weaponText":"Bow",
                                 "description":"A reliable outrider.",
                                 "images":{"hoyowiki_icon":"https://example.test/amber.png"}}
                                """));
        server.createContext("/v5/talents", exchange -> respond(exchange, """
                {"combat1":{"name":"Sharpshooter","description":"A bow attack."},
                 "combat2":{"name":"Explosive Puppet","description":"Deploys Baron Bunny."},
                 "passive1":{"name":"Gliding Champion","description":"Reduces gliding stamina."}}
                """));
        server.createContext("/v5/weapons", exchange -> respond(exchange,
                exchange.getRequestURI().getRawQuery().contains("query=names")
                        ? "[\"Favonius Warbow\"]"
                        : """
                                {"name":"Favonius Warbow","weaponText":"Bow","rarity":4,
                                 "baseAtkValue":41.0671,
                                 "r1":{"description":"CRIT hits generate energy."}}
                                """));
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
        assertEquals(List.of("Amber", "Lisa"), client.listCharacterIds());
        assertEquals(List.of("Favonius Warbow"), client.listWeaponIds());

        Character character = client.getCharacter("Amber");
        assertEquals("Amber", character.getName());
        assertEquals("Pyro", character.getProfile().element());
        assertEquals("Bow", character.getProfile().weaponType());
        assertEquals(3, character.getProfile().skills().size());
        assertEquals("https://example.test/amber.png", character.getProfile().imageUrl());
        assertTrue(character.getHealth() >= 1600 && character.getHealth() <= 2000);
        assertTrue(character.getArmor() >= 120 && character.getArmor() <= 170);
        assertTrue(character.getAttack() >= 180 && character.getAttack() <= 245);

        Equipment weapon = client.getWeapon("Favonius Warbow");
        assertEquals("Favonius Warbow", weapon.getName());
        assertEquals(41, weapon.getAttackBonus());
        assertEquals("Bow", weapon.getWeaponType());
        assertTrue(weapon.getDescription().contains("CRIT"));
    }

    @Test
    void parsesApiJsonIntoImmutableCatalogEntryWithoutInventingCombatStats() throws IOException {
        CharacterCatalogEntry entry = client.getCharacterCatalogEntry("Amber");

        assertEquals("Amber", entry.id());
        assertEquals("Amber", entry.name());
        assertEquals("A reliable outrider.", entry.description());
        assertEquals(4, entry.rarity());
        assertEquals("Pyro", entry.element());
        assertEquals("Bow", entry.weaponType());
        assertEquals("https://example.test/amber.png", entry.imageUrl());
        assertTrue(entry.skills().isEmpty());
        CharacterCatalogEntry withSkill = new CharacterCatalogEntry(
                entry.id(), entry.name(), entry.description(), entry.rarity(), entry.element(),
                entry.weaponType(), entry.imageUrl(),
                List.of(new CharacterSkill("Sharpshooter", "A bow attack.", "Combat")));
        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> withSkill.skills().add(withSkill.skills().get(0)));
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
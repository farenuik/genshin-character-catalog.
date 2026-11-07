package com.myproject;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.myproject.api.GenshinDataSource;
import com.myproject.game.Character;
import com.myproject.game.CharacterProfile;
import com.myproject.game.CharacterSkill;
import com.myproject.game.Equipment;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Test;

class GameCliTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void importsAndDisplaysRemoteEntitiesThenEquipsWeapon() throws IOException {
        GenshinDataSource dataSource = new GenshinDataSource() {
            @Override
            public List<String> listCharacterIds() {
                return List.of("amber");
            }

            @Override
            public List<String> listWeaponIds() {
                return List.of("favonius-warbow");
            }

            @Override
            public Character getCharacter(String id) {
                return new Character("Amber", 1500, 100, 80,
                        new CharacterProfile(id, "A reliable outrider.", 4, "Pyro", "Bow",
                                List.of(new CharacterSkill("Explosive Puppet", "Deploys Baron Bunny.", "Combat"))));
            }

            @Override
            public Equipment getWeapon(String id) {
                return new Equipment("Favonius Warbow", 41, 0, null,
                        "CRIT hits generate energy.", 4, "Bow");
            }
        };
        String commands = "catalog characters\ncatalog weapons\ncreate char amber\n"
                + "create item favonius-warbow\nadd --char_id amber --id favonius-warbow\n"
                + "ls char --id amber\nexit\n";
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;

        try (PrintStream capturedOut = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(capturedOut);
            new GameCli(new BufferedReader(new StringReader(commands)),
                    new com.myproject.game.DefaultAbilityProvider(), dataSource,
                    new CharacterCatalogStore(temporaryDirectory.resolve("characters.json")),
                    imageUrl -> temporaryDirectory.resolve("amber.img")).run();
        } finally {
            System.setOut(originalOut);
        }

        String result = output.toString(StandardCharsets.UTF_8);
        assertTrue(result.contains("- amber"));
        assertTrue(result.contains("- favonius-warbow"));
        assertTrue(result.contains("A reliable outrider."));
        assertTrue(result.contains("Explosive Puppet"));
        assertTrue(result.contains("Favonius Warbow"));
        assertTrue(result.contains("Attack: 121"));
        assertTrue(result.contains("Graphic (https://genshin.jmp.blue/characters/amber/icon)"));
    }

    @Test
    void createsAndReloadsUserCharacterWithItsGraphicLink() throws IOException {
        GenshinDataSource emptyDataSource = new GenshinDataSource() {
            @Override
            public List<String> listCharacterIds() {
                return List.of();
            }

            @Override
            public List<String> listWeaponIds() {
                return List.of();
            }

            @Override
            public Character getCharacter(String id) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Equipment getWeapon(String id) {
                throw new UnsupportedOperationException();
            }
        };
        Path storeFile = temporaryDirectory.resolve("catalog").resolve("characters.json");
        CharacterCatalogStore store = new CharacterCatalogStore(storeFile);
        String createCommands = "create char\nTraveler\n1200\n50\n75\nA custom hero.\nhttps://example.test/traveler.png\nexit\n";

        runCli(createCommands, emptyDataSource, store);

        assertTrue(java.nio.file.Files.exists(storeFile));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try (PrintStream capturedOut = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(capturedOut);
            new GameCli(new BufferedReader(new StringReader("ls char\nls char --id Traveler\nexit\n")),
                    new com.myproject.game.DefaultAbilityProvider(), emptyDataSource, store,
                    imageUrl -> temporaryDirectory.resolve("traveler.img")).run();
        } finally {
            System.setOut(originalOut);
        }

        String result = output.toString(StandardCharsets.UTF_8);
        assertTrue(result.contains("Traveler"));
        assertTrue(result.contains("A custom hero."));
        assertTrue(result.contains("Graphic (https://example.test/traveler.png)"));
    }

    private void runCli(String commands, GenshinDataSource dataSource, CharacterCatalogStore store)
            throws IOException {
        new GameCli(new BufferedReader(new StringReader(commands)),
                new com.myproject.game.DefaultAbilityProvider(), dataSource, store,
                imageUrl -> temporaryDirectory.resolve("created.img")).run();
    }
}
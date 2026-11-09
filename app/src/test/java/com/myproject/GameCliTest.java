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

    @Test
    void runsTurnBasedGameAndStopsAcceptingActionsAfterWinnerIsDeclared() throws IOException {
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
        String commands = "create char A 100 0 60\ncreate char B 100 0 60\n"
                + "game start A B\nturn attack B\nturn defend\nturn attack B\nturn attack A\n"
                + "turn attack B\nturn attack B\ngame status\nexit\n";

        String result = runCliAndCapture(commands, emptyDataSource,
                new CharacterCatalogStore(temporaryDirectory.resolve("characters.json")));

        assertTrue(result.contains("Current turn: A"));
        assertTrue(result.contains("Next turn: B"));
        assertTrue(result.contains("B attacks A for 60 damage."));
        assertTrue(result.contains("Winner: A"));
        assertTrue(result.contains("Error: The game is over; winner: A"));
        assertTrue(result.contains("B: HP=0"));
    }

    @Test
    void savesGameStateHistoryAndAbilitiesForLoadingInANewCli() throws IOException {
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
        CharacterCatalogStore characterStore =
                new CharacterCatalogStore(temporaryDirectory.resolve("characters.json"));
        GameSaveStore gameSaveStore = new GameSaveStore(temporaryDirectory.resolve("last-game.json"));

        String firstRun = runCliAndCapture(
                "create char A 120 0 60\ncreate char B 100 0 60\n"
                        + "create ability spark attack\nadd --char_id A --id spark\n"
                        + "game start A B\nturn ability spark B\nturn defend\nexit\n",
                emptyDataSource, characterStore, gameSaveStore);

        assertTrue(java.nio.file.Files.exists(temporaryDirectory.resolve("last-game.json")));
        assertTrue(firstRun.contains("A casts Fireball on B for 12 damage."));

        String resumedRun = runCliAndCapture(
                "game load\ngame status\ngame history\nturn attack B\ngame history\nexit\n",
                emptyDataSource, characterStore, gameSaveStore);

        assertTrue(resumedRun.contains("Game loaded."));
        assertTrue(resumedRun.contains("B: HP=88"));
        assertTrue(resumedRun.contains("Current turn: A"));
        assertTrue(resumedRun.contains("Turn 1 - A: A casts Fireball on B for 12 damage."));
        assertTrue(resumedRun.contains("Turn 2 - B: B braces for the next attack."));
        assertTrue(resumedRun.contains("Turn 3 - A: A attacks B for 30 damage."));
        assertTrue(resumedRun.contains("Next turn: B"));
    }

    private String runCliAndCapture(String commands, GenshinDataSource dataSource, CharacterCatalogStore store)
            throws IOException {
        return runCliAndCapture(
                commands,
                dataSource,
                store,
                new GameSaveStore(temporaryDirectory.resolve("test-game-save.json")));
    }

    private String runCliAndCapture(
            String commands,
            GenshinDataSource dataSource,
            CharacterCatalogStore store,
            GameSaveStore gameSaveStore) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try (PrintStream capturedOut = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(capturedOut);
            new GameCli(new BufferedReader(new StringReader(commands)),
                    new com.myproject.game.DefaultAbilityProvider(), dataSource, store,
                    imageUrl -> temporaryDirectory.resolve("created.img"), gameSaveStore).run();
        } finally {
            System.setOut(originalOut);
        }
        return output.toString(StandardCharsets.UTF_8);
    }

    private void runCli(String commands, GenshinDataSource dataSource, CharacterCatalogStore store)
            throws IOException {
        new GameCli(new BufferedReader(new StringReader(commands)),
                new com.myproject.game.DefaultAbilityProvider(), dataSource, store,
                imageUrl -> temporaryDirectory.resolve("created.img")).run();
    }
}
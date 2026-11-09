package com.myproject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myproject.game.Ability;
import com.myproject.game.AbilityProvider;
import com.myproject.game.Character;
import com.myproject.game.CharacterProfile;
import com.myproject.game.Game;
import com.myproject.game.GameMemento;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** JSON persistence for the most recently played game and its turn history. */
public final class GameSaveStore {
    private static final int FORMAT_VERSION = 1;

    private final Path file;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GameSaveStore(Path file) {
        this.file = Objects.requireNonNull(file, "file").toAbsolutePath();
    }

    public static GameSaveStore defaultStore() {
        return new GameSaveStore(Path.of(
                System.getProperty("user.home"), ".genshin-cli", "last-game.json"));
    }

    public boolean hasSave() {
        return Files.isRegularFile(file);
    }

    public void save(
            Game game,
            Map<Character, String> playerIds,
            List<String> history,
            Map<Ability, String> abilityTypes) throws IOException {
        Objects.requireNonNull(game, "game");
        Objects.requireNonNull(playerIds, "playerIds");
        Objects.requireNonNull(history, "history");
        Objects.requireNonNull(abilityTypes, "abilityTypes");

        List<SavedPlayer> players = new ArrayList<>();
        for (Character player : game.getPlayers()) {
            String id = playerIds.get(player);
            if (id == null || id.isBlank()) {
                throw new IOException("Cannot save game: a player has no catalog ID");
            }
            Map<String, String> savedAbilities = new LinkedHashMap<>();
            for (Map.Entry<String, Ability> ability : player.getAbilities().entrySet()) {
                String type = abilityTypes.get(ability.getValue());
                if (type == null || type.isBlank()) {
                    throw new IOException("Cannot save game: ability type is unknown for " + ability.getKey());
                }
                savedAbilities.put(ability.getKey(), type);
            }
            players.add(new SavedPlayer(
                    id,
                    player.getName(),
                    player.getMaxHealth(),
                    player.getHealth(),
                    player.getArmor(),
                    player.getAttack(),
                    player.isDefending(),
                    player.getProfile(),
                    savedAbilities));
        }

        int currentPlayerIndex = game.getPlayers().indexOf(game.getCurrentPlayer());
        SavedGame savedGame = new SavedGame(FORMAT_VERSION, players, currentPlayerIndex, List.copyOf(history));
        Files.createDirectories(file.getParent());
        Path temporaryFile = Files.createTempFile(file.getParent(), "game-", ".json.tmp");
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(temporaryFile.toFile(), savedGame);
            try {
                Files.move(temporaryFile, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    public LoadedGame load(AbilityProvider abilityProvider) throws IOException {
        Objects.requireNonNull(abilityProvider, "abilityProvider");
        if (!hasSave()) {
            throw new IOException("No saved game found at " + file);
        }

        SavedGame savedGame = objectMapper.readValue(file.toFile(), SavedGame.class);
        if (savedGame == null) {
            throw new IOException("Saved game is empty: " + file);
        }
        if (savedGame.version() != FORMAT_VERSION) {
            throw new IOException("Unsupported saved game format version: " + savedGame.version());
        }
        if (savedGame.players() == null || savedGame.players().size() < 2
                || savedGame.currentPlayerIndex() < 0
                || savedGame.currentPlayerIndex() >= savedGame.players().size()) {
            throw new IOException("Invalid saved game data in " + file);
        }

        List<Character> players = new ArrayList<>();
        Map<Character, String> playerIds = new LinkedHashMap<>();
        Map<Ability, String> abilityTypes = new IdentityHashMap<>();
        for (SavedPlayer savedPlayer : savedGame.players()) {
            if (savedPlayer == null || savedPlayer.id() == null || savedPlayer.id().isBlank()
                    || savedPlayer.name() == null || savedPlayer.name().isBlank()
                    || savedPlayer.profile() == null
                    || savedPlayer.abilities() == null
                    || savedPlayer.maxHealth() <= 0
                    || savedPlayer.health() < 0
                    || savedPlayer.health() > savedPlayer.maxHealth()
                    || savedPlayer.armor() < 0
                    || savedPlayer.attack() < 0
                    || playerIds.containsValue(savedPlayer.id())) {
                throw new IOException("Invalid player data in saved game: " + file);
            }

            Character player;
            try {
                player = new Character(
                        savedPlayer.name(),
                        savedPlayer.maxHealth(),
                        savedPlayer.armor(),
                        savedPlayer.attack(),
                        savedPlayer.profile());
                player.restoreCombatState(
                        savedPlayer.health(), savedPlayer.armor(), savedPlayer.attack(), savedPlayer.defending());
                for (Map.Entry<String, String> ability : savedPlayer.abilities().entrySet()) {
                    if (ability.getKey() == null || ability.getKey().isBlank()
                            || ability.getValue() == null || ability.getValue().isBlank()) {
                        throw new IOException("Invalid ability data in saved game: " + file);
                    }
                    Ability restoredAbility = abilityProvider.create(ability.getValue());
                    player.learn(ability.getKey(), restoredAbility);
                    abilityTypes.put(restoredAbility, ability.getValue());
                }
            } catch (IllegalArgumentException exception) {
                throw new IOException("Invalid player or ability data in saved game: " + file, exception);
            }
            players.add(player);
            playerIds.put(player, savedPlayer.id());
        }

        Game game = new Game();
        game.restoreState(GameMemento.capture(players, savedGame.currentPlayerIndex()));
        List<String> history = savedGame.history() == null ? List.of() : List.copyOf(savedGame.history());
        return new LoadedGame(game, Map.copyOf(playerIds), history, Map.copyOf(abilityTypes));
    }

    public record LoadedGame(
            Game game,
            Map<Character, String> playerIds,
            List<String> history,
            Map<Ability, String> abilityTypes) {
        public LoadedGame {
            Objects.requireNonNull(game, "game");
            playerIds = Map.copyOf(playerIds);
            history = List.copyOf(history);
            abilityTypes = Map.copyOf(abilityTypes);
        }
    }

    private record SavedGame(
            int version,
            List<SavedPlayer> players,
            int currentPlayerIndex,
            List<String> history) { }

    private record SavedPlayer(
            String id,
            String name,
            int maxHealth,
            int health,
            int armor,
            int attack,
            boolean defending,
            CharacterProfile profile,
            Map<String, String> abilities) { }
}

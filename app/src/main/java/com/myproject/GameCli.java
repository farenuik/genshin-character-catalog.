package com.myproject;

import com.myproject.api.GenshinApiClient;
import com.myproject.api.GenshinDataSource;
import com.myproject.game.Ability;
import com.myproject.game.AbilityProvider;
import com.myproject.game.Character;
import com.myproject.game.CharacterProfile;
import com.myproject.game.DefaultAbilityProvider;
import com.myproject.game.Equipment;
import com.myproject.game.Game;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class GameCli {
    private final BufferedReader input;
    private final AbilityProvider abilityProvider;
    private final GenshinDataSource dataSource;
    private final CharacterCatalogStore characterStore;
    private final PortraitCache portraitCache;
    private final GameSaveStore gameSaveStore;
    private final Map<String, Character> characters = new LinkedHashMap<>();
    private final Map<String, Equipment> items = new LinkedHashMap<>();
    private final Map<String, Ability> abilities = new LinkedHashMap<>();
    private final Map<Ability, String> abilityTypes = new IdentityHashMap<>();
    private final Map<Character, String> gamePlayerIds = new IdentityHashMap<>();
    private final List<String> gameHistory = new ArrayList<>();
    private Game game;

    public GameCli(BufferedReader input) {
        this(input, new DefaultAbilityProvider(), new GenshinApiClient());
    }

    public GameCli(BufferedReader input, AbilityProvider abilityProvider) {
        this(input, abilityProvider, new GenshinApiClient());
    }

    public GameCli(BufferedReader input, AbilityProvider abilityProvider, GenshinDataSource dataSource) {
        this(input, abilityProvider, dataSource, CharacterCatalogStore.defaultStore());
    }

    public GameCli(
            BufferedReader input,
            AbilityProvider abilityProvider,
            GenshinDataSource dataSource,
            CharacterCatalogStore characterStore) {
        this(input, abilityProvider, dataSource, characterStore, FilePortraitCache.defaultCache());
    }

    public GameCli(
            BufferedReader input,
            AbilityProvider abilityProvider,
            GenshinDataSource dataSource,
            CharacterCatalogStore characterStore,
            PortraitCache portraitCache) {
        this(input, abilityProvider, dataSource, characterStore, portraitCache, GameSaveStore.defaultStore());
    }

    public GameCli(
            BufferedReader input,
            AbilityProvider abilityProvider,
            GenshinDataSource dataSource,
            CharacterCatalogStore characterStore,
            PortraitCache portraitCache,
            GameSaveStore gameSaveStore) {
        this.input = Objects.requireNonNull(input, "input");
        this.abilityProvider = Objects.requireNonNull(abilityProvider, "abilityProvider");
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.characterStore = Objects.requireNonNull(characterStore, "characterStore");
        this.portraitCache = Objects.requireNonNull(portraitCache, "portraitCache");
        this.gameSaveStore = Objects.requireNonNull(gameSaveStore, "gameSaveStore");
    }

    public void run() throws IOException {
        characters.putAll(characterStore.load());
        CommandRegistry commands = new CommandRegistry()
                .register("create", this::create)
                .register("add", this::add)
                .register("act", this::act)
                .register("game", this::game)
                .register("turn", this::turn)
                .register("ls", this::ls)
                .register("catalog", this::catalog)
                .register("help", arguments -> System.out.println(
                    "create char <api-id> | create char (interactive: name, stats, description, graphic URL) | " +
                    "create item <api-id> | create <item|ability> | catalog <characters|weapons|refresh> | " +
                    "add [--char_id <id/name> --id <id/name>] | " +
                    "game start [player-1 player-2] | game status | game history | " +
                    "game save | game load | game stop | " +
                    "turn <attack [target]|defend|heal|ability <name> [target]> | " +
                    "act <attack|heal|ability> <actor> <target> [--id <id/name>] | " +
                    "ls <char|item|ability> [--id <id/name>] | exit"));

        System.out.println("Characters mode. Commands: " + commands.help());
        while (true) {
            System.out.print("> ");
            String line = input.readLine();
            if (line == null || !commands.execute(line)) {
                return;
            }
        }
    }

    private void create(List<String> arguments) throws IOException {
        if (arguments.isEmpty()) {
            throw new IllegalArgumentException("Usage: create <char|item|ability>");
        }

        String kind = arguments.get(0);
        switch (kind) {
            case "char" -> createCharacter(arguments);
            case "item" -> createItem(arguments);
            case "ability" -> createAbility(arguments);
            default -> throw new IllegalArgumentException("Unsupported type: " + kind);
        }
    }

    private void createCharacter(List<String> arguments) throws IOException {
        if (arguments.size() >= 5) {
            String name = arguments.get(1);
            int health = Integer.parseInt(arguments.get(2));
            int armor = Integer.parseInt(arguments.get(3));
            int attack = Integer.parseInt(arguments.get(4));
            Character character = new Character(name, health, armor, attack,
                    new CharacterProfile("", "", 0, "", "", List.of()));
            characters.put(name, character);
            saveCharacters();
            System.out.println("Character created: " + name);
            return;
        }

        if (arguments.size() == 2) {
            String id = arguments.get(1);
            Character character = dataSource.getCharacter(id);
            characters.put(id, character);
            saveCharacters();
            cachePortrait(character);
            System.out.println("Character imported: " + character.getName() + " (" + id + ")");
            printCharacter(character);
            return;
        }
        if (arguments.size() > 1) {
            throw new IllegalArgumentException("Usage: create char <api-id> | create char <name> <hp> <armor> <attack>");
        }

        System.out.print("Name: ");
        String name = readLine();
        System.out.print("Health: ");
        int health = Integer.parseInt(readLine());
        System.out.print("Armor: ");
        int armor = Integer.parseInt(readLine());
        System.out.print("Attack: ");
        int attack = Integer.parseInt(readLine());
        System.out.print("Description (optional): ");
        String description = readLine();
        System.out.print("Graphic URL: ");
        String imageUrl = readLine();
        if (imageUrl.isBlank()) {
            throw new IllegalArgumentException("Graphic URL must not be blank");
        }

        CharacterProfile profile = new CharacterProfile("", description, 0, "", "", List.of(), imageUrl);
        Character character = new Character(name, health, armor, attack, profile);
        characters.put(name, character);
        saveCharacters();
        cachePortrait(character);
        System.out.println("Character created: " + name);
    }

    private void cachePortrait(Character character) {
        String imageUrl = character.getProfile().imageUrl();
        if (imageUrl.isBlank()) return;

        try {
            System.out.println("Portrait cached: " + portraitCache.cache(imageUrl));
        } catch (IOException exception) {
            System.err.println("Could not cache portrait for " + character.getName()
                    + ": " + exception.getMessage());
        }
    }

    private void createItem(List<String> arguments) throws IOException {
        if (arguments.size() >= 5) {
            String itemId = arguments.get(1);
            int attackBonus = Integer.parseInt(arguments.get(2));
            int armorBonus = Integer.parseInt(arguments.get(3));
            String abilityId = arguments.size() >= 5 ? arguments.get(4) : null;
            Ability granted = abilityId == null || abilityId.isBlank() ? null : abilities.get(abilityId);
            if (abilityId != null && !abilityId.isBlank() && granted == null) {
                throw new IllegalArgumentException("Ability not found: " + abilityId);
            }
            Equipment equipment = new Equipment(itemId, attackBonus, armorBonus, granted);
            items.put(itemId, equipment);
            System.out.println("Item created: " + itemId);
            return;
        }

        if (arguments.size() == 2) {
            String id = arguments.get(1);
            Equipment equipment = dataSource.getWeapon(id);
            items.put(id, equipment);
            System.out.println("Weapon imported: " + equipment.getName() + " (" + id + ")");
            System.out.println(equipment.getWeaponType() + ", rarity=" + equipment.getRarity()
                    + ", base ATK=" + equipment.getAttackBonus());
            return;
        }

        System.out.print("Item id/name: ");
        String itemId = readLine();
        System.out.print("Attack bonus: ");
        int attackBonus = Integer.parseInt(readLine());
        System.out.print("Armor bonus: ");
        int armorBonus = Integer.parseInt(readLine());
        System.out.print("Grant ability id (optional): ");
        String abilityId = readLine();

        Ability granted = abilityId == null || abilityId.isBlank() ? null : abilities.get(abilityId);
        if (abilityId != null && !abilityId.isBlank() && granted == null) {
            throw new IllegalArgumentException("Ability not found: " + abilityId);
        }

        Equipment equipment = new Equipment(itemId, attackBonus, armorBonus, granted);
        items.put(itemId, equipment);
        System.out.println("Item created: " + itemId);
    }

    private void createAbility(List<String> arguments) throws IOException {
        if (arguments.size() >= 3) {
            String abilityId = arguments.get(1);
            String type = arguments.get(2);
            Ability ability = abilityProvider.create(type);
            abilities.put(abilityId, ability);
            abilityTypes.put(ability, type);
            System.out.println("Ability created: " + abilityId);
            return;
        }

        System.out.print("Ability id/name: ");
        String abilityId = readLine();
        System.out.print("Type [attack|heal]: ");
        String type = readLine();

        Ability ability = abilityProvider.create(type);

        abilities.put(abilityId, ability);
        abilityTypes.put(ability, type);
        System.out.println("Ability created: " + abilityId);
    }

    private void add(List<String> arguments) {
        String characterId = resolveOption(arguments, "--char_id");
        String targetId = resolveOption(arguments, "--id");

        if (characterId == null || targetId == null) {
            throw new IllegalArgumentException("Usage: add [--char_id <id/name> --id <id/name>]");
        }

        Character character = resolveCharacter(characterId);
        Equipment equipment = items.get(targetId);
        if (equipment != null) {
            character.equip(equipment);
            saveCharacters();
            System.out.println("Added item " + targetId + " to " + character.getName());
            printCharacter(character);
            return;
        }

        Ability ability = abilities.get(targetId);
        if (ability != null) {
            character.learn(targetId, ability);
            System.out.println("Added ability " + targetId + " to " + character.getName());
            return;
        }

        throw new IllegalArgumentException("Item or ability not found: " + targetId);
    }

    private void act(List<String> arguments) {
        if (arguments.size() < 3) {
            throw new IllegalArgumentException("Usage: act <attack|heal|ability> <actor> <target> [--id <id/name>]");
        }

        String action = arguments.get(0);
        Character actor = resolveCharacter(arguments.get(1));
        Character target = resolveCharacter(arguments.get(2));
        String id = resolveOption(arguments, "--id");

        switch (action) {
            case "attack" -> System.out.println(actor.attack(target));
            case "heal" -> System.out.println(actor.heal(18));
            case "ability" -> {
                if (id == null) {
                    if (actor.getAbilities().isEmpty()) {
                        throw new IllegalArgumentException(actor.getName() + " has no abilities");
                    }
                    id = actor.getAbilities().keySet().iterator().next();
                }
                System.out.println(actor.useAbility(id, target));
            }
            default -> throw new IllegalArgumentException("Unsupported action: " + action);
        }
        saveCharacters();
    }

    private void game(List<String> arguments) throws IOException {
        if (arguments.isEmpty()) {
            throw new IllegalArgumentException("Usage: game <start|status|stop>");
        }

        switch (arguments.get(0)) {
            case "start" -> startGame(arguments.subList(1, arguments.size()));
            case "status" -> printGameStatus();
            case "history" -> printGameHistory();
            case "save" -> {
                saveGame();
                System.out.println("Game saved.");
            }
            case "load" -> loadGame();
            case "stop" -> {
                if (game == null) {
                    throw new IllegalStateException("No game is running");
                }
                saveGame();
                game = null;
                gamePlayerIds.clear();
                gameHistory.clear();
                System.out.println("Game paused; use game load to resume.");
            }
            default -> throw new IllegalArgumentException("Unsupported game command: " + arguments.get(0));
        }
    }

    private void startGame(List<String> playerIds) throws IOException {
        if (game != null && !game.isOver()) {
            throw new IllegalStateException("A game is already running; use game stop first");
        }
        if (!playerIds.isEmpty() && playerIds.size() != 2) {
            throw new IllegalArgumentException("Usage: game start [player-1 player-2]");
        }
        if (characters.size() < 2) {
            throw new IllegalStateException("Create or import at least two characters before starting a game");
        }

        Character first;
        Character second;
        if (playerIds.isEmpty()) {
            printAvailableCharacters();
            System.out.print("First player: ");
            first = resolveCharacter(readLine());
            System.out.print("Second player: ");
            second = resolveCharacter(readLine());
        } else {
            first = resolveCharacter(playerIds.get(0));
            second = resolveCharacter(playerIds.get(1));
        }
        if (first == second) {
            throw new IllegalArgumentException("Choose two different characters");
        }
        if (first.getHealth() <= 0 || second.getHealth() <= 0) {
            throw new IllegalArgumentException("Both characters must have more than 0 HP");
        }

        Game newGame = new Game();
        Character firstPlayer = copyForGame(first);
        Character secondPlayer = copyForGame(second);
        newGame.addPlayer(firstPlayer);
        newGame.addPlayer(secondPlayer);
        newGame.start();
        game = newGame;
        gamePlayerIds.clear();
        gamePlayerIds.put(firstPlayer, playerIds.isEmpty()
                ? findCharacterId(first) : playerIds.get(0));
        gamePlayerIds.put(secondPlayer, playerIds.isEmpty()
                ? findCharacterId(second) : playerIds.get(1));
        gameHistory.clear();
        gameHistory.add("Game started: " + firstPlayer.getName() + " vs " + secondPlayer.getName());
        saveGame();
        System.out.println("Game started: " + firstPlayer.getName() + " vs " + secondPlayer.getName());
        printGameStatus();
        System.out.println("Use turn <attack [target]|defend|heal|ability <name> [target]>.");
    }

    private void turn(List<String> arguments) {
        if (game == null) {
            throw new IllegalStateException("Start a game first with game start");
        }
        if (game.isOver()) {
            throw new IllegalStateException("The game is over; winner: " + game.getWinner().getName());
        }
        if (arguments.isEmpty()) {
            throw new IllegalArgumentException(
                    "Usage: turn <attack [target]|defend|heal|ability <name> [target]>");
        }

        Character currentPlayer = game.getCurrentPlayer();
        Character opponent = game.getPlayers().stream()
                .filter(player -> player != currentPlayer)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("The game has no opponent"));

        String result = switch (arguments.get(0)) {
            case "attack" -> {
                if (arguments.size() > 2) {
                    throw new IllegalArgumentException("Usage: turn attack [target]");
                }
                Character target = arguments.size() == 2 ? resolveGamePlayer(arguments.get(1)) : opponent;
                yield game.attack(target);
            }
            case "defend" -> {
                requireArgumentCount(arguments, 1, "turn defend");
                yield game.defend();
            }
            case "heal" -> {
                requireArgumentCount(arguments, 1, "turn heal");
                yield game.heal(18);
            }
            case "ability" -> {
                if (arguments.size() < 2 || arguments.size() > 3) {
                    throw new IllegalArgumentException("Usage: turn ability <name> [target]");
                }
                Character target = arguments.size() == 3
                        ? resolveGamePlayer(arguments.get(2))
                        : opponent;
                yield game.useAbility(arguments.get(1), target);
            }
            default -> throw new IllegalArgumentException("Unsupported turn action: " + arguments.get(0));
        };

        System.out.println(result);
        long turnNumber = gameHistory.stream().filter(entry -> entry.startsWith("Turn ")).count() + 1;
        gameHistory.add("Turn " + turnNumber + " - " + currentPlayer.getName() + ": " + result);
        if (game.isOver()) {
            String winner = game.getWinner().getName();
            gameHistory.add("Winner: " + winner);
            System.out.println("Winner: " + winner);
        } else {
            System.out.println("Next turn: " + game.getCurrentPlayer().getName());
        }
        saveGame();
    }

    private Character resolveGamePlayer(String id) {
        for (Character player : game.getPlayers()) {
            String playerId = gamePlayerIds.get(player);
            if ((playerId != null && playerId.equalsIgnoreCase(id))
                    || player.getName().equalsIgnoreCase(id)) {
                if (player.getHealth() <= 0) {
                    throw new IllegalArgumentException(player.getName() + " has been defeated");
                }
                return player;
            }
        }
        throw new IllegalArgumentException(id + " is not a player in this game");
    }

    private void printAvailableCharacters() {
        System.out.println("Available characters:");
        characters.forEach((id, character) ->
                System.out.println("- " + id + " (" + character.getName() + ", HP=" + character.getHealth() + ")"));
    }

    private void printGameStatus() {
        if (game == null) {
            System.out.println("No game is running.");
            return;
        }
        for (Character player : game.getPlayers()) {
            System.out.println(player.getName() + ": HP=" + player.getHealth()
                    + ", armor=" + player.getArmor() + ", attack=" + player.getAttack());
        }
        if (game.isOver()) {
            Character winner = game.getWinner();
            System.out.println(winner == null ? "Game ended with no winner." : "Winner: " + winner.getName());
        } else {
            System.out.println("Current turn: " + game.getCurrentPlayer().getName());
        }
    }

    private void printGameHistory() {
        if (gameHistory.isEmpty()) {
            System.out.println("No game history loaded; use game load to open the last save.");
            return;
        }
        System.out.println("Game history:");
        gameHistory.forEach(entry -> System.out.println("- " + entry));
    }

    private void loadGame() throws IOException {
        if (game != null && !game.isOver()) {
            throw new IllegalStateException("Pause the current game with game stop before loading a save");
        }
        GameSaveStore.LoadedGame loadedGame = gameSaveStore.load(abilityProvider);
        game = loadedGame.game();
        gamePlayerIds.clear();
        gamePlayerIds.putAll(loadedGame.playerIds());
        gameHistory.clear();
        gameHistory.addAll(loadedGame.history());
        abilityTypes.putAll(loadedGame.abilityTypes());
        System.out.println("Game loaded.");
        printGameStatus();
    }

    private Character copyForGame(Character character) {
        Character copy = new Character(
                character.getName(),
                character.getMaxHealth(),
                character.getArmor(),
                character.getAttack(),
                character.getProfile());
        copy.restoreCombatState(
                character.getHealth(), character.getArmor(), character.getAttack(), character.isDefending());
        character.getAbilities().forEach(copy::learn);
        return copy;
    }

    private String findCharacterId(Character character) {
        return characters.entrySet().stream()
                .filter(entry -> entry.getValue() == character)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Character is not in the local catalog: " + character.getName()));
    }

    private void saveGame() {
        if (game == null) {
            throw new IllegalStateException("There is no game to save");
        }
        try {
            gameSaveStore.save(game, gamePlayerIds, gameHistory, abilityTypes);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save game: " + exception.getMessage(), exception);
        }
    }

    private static void requireArgumentCount(List<String> arguments, int expected, String usage) {
        if (arguments.size() != expected) {
            throw new IllegalArgumentException("Usage: " + usage);
        }
    }

    private void ls(List<String> arguments) {
        if (arguments.isEmpty()) {
            System.out.println("Characters:");
            for (Character character : characters.values()) {
                System.out.println("- " + character.getName() + " hp=" + character.getHealth()
                            + ", armor=" + character.getArmor() + ", attack=" + character.getAttack()
                            + ", graphic=" + graphicOrUnavailable(character));
            }
            System.out.println("Items:");
            for (String itemId : items.keySet()) {
                Equipment item = items.get(itemId);
                System.out.println("- " + itemId + " attack+" + item.getAttackBonus() + ", armor+" + item.getArmorBonus());
            }
            System.out.println("Abilities:");
            for (String abilityId : abilities.keySet()) {
                System.out.println("- " + abilityId);
            }
            return;
        }

        String kind = arguments.get(0);
        String id = resolveOption(arguments, "--id");
        if (kind.equals("char")) {
            if (id != null) {
                Character character = resolveCharacter(id);
                printCharacter(character);
                return;
            }
            for (Character character : characters.values()) {
                System.out.println(character.getName() + " hp=" + character.getHealth()
                        + ", armor=" + character.getArmor() + ", attack=" + character.getAttack()
                        + ", graphic=" + graphicOrUnavailable(character));
            }
            return;
        }

        if (kind.equals("item")) {
            if (id != null) {
                Equipment equipment = items.get(id);
                if (equipment == null) throw new IllegalArgumentException("Item not found: " + id);
                System.out.println(id + " attack+" + equipment.getAttackBonus() + ", armor+" + equipment.getArmorBonus()
                        + ", users=" + findUsersForItem(id));
                return;
            }
            for (String itemId : items.keySet()) {
                Equipment equipment = items.get(itemId);
                System.out.println(itemId + " attack+" + equipment.getAttackBonus() + ", armor+" + equipment.getArmorBonus());
            }
            return;
        }

        if (kind.equals("ability")) {
            if (id != null) {
                Ability ability = abilities.get(id);
                if (ability == null) throw new IllegalArgumentException("Ability not found: " + id);
                System.out.println(id + ", users=" + findUsersForAbility(id));
                return;
            }
            for (String abilityId : abilities.keySet()) {
                System.out.println(abilityId + ", users=" + findUsersForAbility(abilityId));
            }
            return;
        }

        throw new IllegalArgumentException("Unsupported category: " + kind);
    }

    private void catalog(List<String> arguments) throws IOException {
        if (arguments.size() != 1) {
            throw new IllegalArgumentException("Usage: catalog <characters|weapons|refresh>");
        }
        String kind = arguments.get(0);
        List<String> ids = switch (kind) {
            case "characters", "refresh" -> dataSource.listCharacterIds();
            case "weapons" -> dataSource.listWeaponIds();
            default -> throw new IllegalArgumentException("Unsupported catalog: " + kind);
        };
        System.out.println((kind.equals("refresh") ? "characters" : kind) + " in Genshin API:");
        ids.forEach(id -> System.out.println("- " + id));
    }

    private static void printCharacter(Character character) {
        System.out.println(new CharacterTextRenderer().render(character));
    }

    private static String graphicOrUnavailable(Character character) {
        String graphicUrl = CharacterTextRenderer.graphicUrl(character);
        return graphicUrl.isBlank() ? "not provided" : graphicUrl;
    }

    private String findUsersForItem(String itemId) {
        List<String> users = new ArrayList<>();
        for (Map.Entry<String, Character> entry : characters.entrySet()) {
            Character character = entry.getValue();
            if (character.getAbilities().containsKey(itemId)) {
                users.add(entry.getKey());
            }
        }
        return users.isEmpty() ? "none" : String.join(", ", users);
    }

    private String findUsersForAbility(String abilityId) {
        List<String> users = new ArrayList<>();
        for (Map.Entry<String, Character> entry : characters.entrySet()) {
            if (entry.getValue().getAbilities().containsKey(abilityId)) {
                users.add(entry.getKey());
            }
        }
        return users.isEmpty() ? "none" : String.join(", ", users);
    }

    private Character resolveCharacter(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Character id must not be blank");
        }
        Character character = characters.get(id);
        if (character == null) {
            for (Character candidate : characters.values()) {
                if (candidate.getName().equalsIgnoreCase(id)) {
                    return candidate;
                }
            }
        }
        if (character == null) {
            throw new IllegalArgumentException("Character not found: " + id);
        }
        return character;
    }

    private static String resolveOption(List<String> args, String optionName) {
        for (int index = 0; index < args.size(); index++) {
            String token = args.get(index);
            if (token.equals(optionName)) {
                if (index + 1 < args.size()) {
                    return args.get(index + 1);
                }
                throw new IllegalArgumentException("Missing value for " + optionName);
            }
            if (token.startsWith(optionName + "=")) {
                return token.substring(optionName.length() + 1);
            }
        }
        return null;
    }

    private String readLine() throws IOException {
        String line = input.readLine();
        if (line == null) {
            throw new IOException("Input closed");
        }
        return line.trim();
    }

    private void saveCharacters() {
        try {
            characterStore.save(characters);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save character catalog: " + exception.getMessage(), exception);
        }
    }
}

package com.myproject;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myproject.game.Character;
import com.myproject.game.CharacterProfile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** JSON persistence for characters created or imported through the CLI. */
public final class CharacterCatalogStore {
    private static final TypeReference<List<SavedCharacter>> SAVED_CHARACTERS = new TypeReference<>() { };

    private final Path file;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CharacterCatalogStore(Path file) {
        this.file = Objects.requireNonNull(file, "file").toAbsolutePath();
    }

    public static CharacterCatalogStore defaultStore() {
        return new CharacterCatalogStore(Path.of(
                System.getProperty("user.home"), ".genshin-cli", "characters.json"));
    }

    public Map<String, Character> load() throws IOException {
        if (Files.notExists(file)) return Map.of();

        List<SavedCharacter> savedCharacters = objectMapper.readValue(file.toFile(), SAVED_CHARACTERS);
        Map<String, Character> characters = new LinkedHashMap<>();
        for (SavedCharacter saved : savedCharacters) {
            if (saved.id() == null || saved.id().isBlank() || saved.profile() == null) {
                throw new IOException("Invalid character data in " + file);
            }
            characters.put(saved.id(), new Character(
                    saved.name(), saved.health(), saved.armor(), saved.attack(), saved.profile()));
        }
        return characters;
    }

    public void save(Map<String, Character> characters) throws IOException {
        Objects.requireNonNull(characters, "characters");
        Files.createDirectories(file.getParent());
        List<SavedCharacter> savedCharacters = new ArrayList<>();
        characters.forEach((id, character) -> savedCharacters.add(new SavedCharacter(
                id,
                character.getName(),
                character.getHealth(),
                character.getArmor(),
                character.getAttack(),
                character.getProfile())));

        Path temporaryFile = Files.createTempFile(file.getParent(), "characters-", ".json.tmp");
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(temporaryFile.toFile(), savedCharacters);
            try {
                Files.move(temporaryFile, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private record SavedCharacter(
            String id,
            String name,
            int health,
            int armor,
            int attack,
            CharacterProfile profile) { }
}

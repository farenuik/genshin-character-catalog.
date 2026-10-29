package com.myproject.api;

import com.myproject.game.Character;
import com.myproject.game.Equipment;
import java.io.IOException;
import java.util.List;

public interface GenshinDataSource {
    List<String> listCharacterIds() throws IOException;

    List<String> listWeaponIds() throws IOException;

    Character getCharacter(String id) throws IOException;

    default CharacterCatalogEntry getCharacterCatalogEntry(String id) throws IOException {
        Character character = getCharacter(id);
        var profile = character.getProfile();
        return new CharacterCatalogEntry(
                profile.apiId(),
                character.getName(),
                profile.description(),
                profile.rarity(),
                profile.element(),
                profile.weaponType(),
                profile.imageUrl(),
                profile.skills());
    }

    Equipment getWeapon(String id) throws IOException;
}
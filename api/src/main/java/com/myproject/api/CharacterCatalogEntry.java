package com.myproject.api;

import com.myproject.game.CharacterSkill;
import java.util.List;

/** Immutable character data parsed from the remote API for catalog presentation. */
public record CharacterCatalogEntry(
        String id,
        String name,
        String description,
        int rarity,
        String element,
        String weaponType,
        String imageUrl,
        List<CharacterSkill> skills) {

    public CharacterCatalogEntry {
        id = id == null ? "" : id;
        name = name == null ? "" : name;
        description = description == null ? "" : description;
        element = element == null ? "" : element;
        weaponType = weaponType == null ? "" : weaponType;
        imageUrl = imageUrl == null ? "" : imageUrl;
        skills = skills == null ? List.of() : List.copyOf(skills);
    }
}

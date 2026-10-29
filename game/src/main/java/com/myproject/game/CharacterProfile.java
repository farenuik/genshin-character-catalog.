package com.myproject.game;

import java.util.List;

public record CharacterProfile(
        String apiId,
        String description,
        int rarity,
        String element,
        String weaponType,
        List<CharacterSkill> skills,
        String imageUrl) {

    public CharacterProfile {
        apiId = apiId == null ? "" : apiId;
        description = description == null ? "" : description;
        element = element == null ? "" : element;
        weaponType = weaponType == null ? "" : weaponType;
        skills = skills == null ? List.of() : List.copyOf(skills);
        imageUrl = imageUrl == null ? "" : imageUrl;
    }

    public CharacterProfile(
            String apiId,
            String description,
            int rarity,
            String element,
            String weaponType,
            List<CharacterSkill> skills) {
        this(apiId, description, rarity, element, weaponType, skills, "");
    }
}
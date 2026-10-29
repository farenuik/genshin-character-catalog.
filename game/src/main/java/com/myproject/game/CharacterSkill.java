package com.myproject.game;

public record CharacterSkill(String name, String description, String type) {
    public CharacterSkill {
        name = name == null ? "" : name;
        description = description == null ? "" : description;
        type = type == null ? "" : type;
    }
}
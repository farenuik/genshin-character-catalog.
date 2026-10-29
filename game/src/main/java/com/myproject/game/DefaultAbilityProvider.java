package com.myproject.game;

/** Default ability provider used by the command-line application. */
public final class DefaultAbilityProvider implements AbilityProvider {
    @Override
    public Ability create(String type) {
        return switch (type) {
            case "attack" -> Abilities.fireball();
            case "heal" -> Abilities.healingLight();
            default -> throw new IllegalArgumentException("Unsupported ability type: " + type);
        };
    }
}
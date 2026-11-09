package com.myproject.game;

/** Immutable notification describing a meaningful game-session change. */
public record GameEvent(Type type, String description) {
    public enum Type {
        PLAYER_ADDED,
        STARTED,
        ATTACK,
        ABILITY_USED,
        DEFENDED,
        HEALED,
        RESTORED
    }
}

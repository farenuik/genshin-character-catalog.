package com.myproject.game;

import java.util.List;

/**
 * Opaque snapshot used by Game to restore a session without exposing its internals.
 */
public final class GameMemento {
    private final List<Character> players;
    private final List<Character.State> playerStates;
    private final int currentPlayerIndex;
    private final boolean started;

    GameMemento(List<Character> players, int currentPlayerIndex, boolean started) {
        this.players = List.copyOf(players);
        this.playerStates = this.players.stream().map(Character::saveState).toList();
        this.currentPlayerIndex = currentPlayerIndex;
        this.started = started;
    }

    /** Captures a started session using its restored roster and active turn. */
    public static GameMemento capture(List<Character> players, int currentPlayerIndex) {
        if (players == null || players.size() < 2
                || currentPlayerIndex < 0 || currentPlayerIndex >= players.size()) {
            throw new IllegalArgumentException("Invalid game state");
        }
        return new GameMemento(players, currentPlayerIndex, true);
    }

    List<Character> players() {
        return players;
    }

    Character.State playerState(int index) {
        return playerStates.get(index);
    }

    int currentPlayerIndex() {
        return currentPlayerIndex;
    }

    boolean started() {
        return started;
    }
}

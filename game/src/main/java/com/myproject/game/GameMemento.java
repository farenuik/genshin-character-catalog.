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

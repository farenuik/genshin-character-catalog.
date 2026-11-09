package com.myproject.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/** Coordinates players, turns, and actions for one game session. */
public final class Game implements GameEngine {
    private final List<Character> players = new ArrayList<>();
    private final List<GameObserver> observers = new CopyOnWriteArrayList<>();
    private int currentPlayerIndex;
    private boolean started;

    public void addPlayer(Character player) {
        if (started) {
            throw new IllegalStateException("Players cannot be added after the game starts");
        }
        Character addedPlayer = Objects.requireNonNull(player, "Player must not be null");
        if (players.contains(addedPlayer)) {
            throw new IllegalArgumentException("A character can only be added once");
        }
        players.add(addedPlayer);
        notifyObservers(new GameEvent(GameEvent.Type.PLAYER_ADDED, addedPlayer.getName()));
    }

    public void start() {
        if (started) {
            throw new IllegalStateException("The game has already started");
        }
        if (players.size() < 2) {
            throw new IllegalStateException("At least two players are required");
        }
        if (players.stream().anyMatch(player -> player.getHealth() <= 0)) {
            throw new IllegalStateException("All players must have more than 0 HP");
        }
        started = true;
        currentPlayerIndex = 0;
        notifyObservers(new GameEvent(GameEvent.Type.STARTED, "Game started"));
    }

    /** Registers an Observer for subsequent game-session events. */
    public void addObserver(GameObserver observer) {
        observers.add(Objects.requireNonNull(observer, "Observer must not be null"));
    }

    /** Stops notifying this Observer. */
    public void removeObserver(GameObserver observer) {
        observers.remove(Objects.requireNonNull(observer, "Observer must not be null"));
    }

    /** Captures the session and each player's mutable combat state. */
    public GameMemento saveState() {
        return new GameMemento(players, currentPlayerIndex, started);
    }

    /** Restores a previously saved session snapshot. */
    public void restoreState(GameMemento memento) {
        Objects.requireNonNull(memento, "Memento must not be null");
        players.clear();
        players.addAll(memento.players());
        for (int index = 0; index < players.size(); index++) {
            players.get(index).restoreState(memento.playerState(index));
        }
        currentPlayerIndex = memento.currentPlayerIndex();
        started = memento.started();
        notifyObservers(new GameEvent(GameEvent.Type.RESTORED, "Game state restored"));
    }

    public Character getCurrentPlayer() {
        ensureStarted();
        return players.get(currentPlayerIndex);
    }

    public List<Character> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    public String attack(Character target) {
        Character attacker = getActivePlayer();
        requireOpponent(target, attacker);
        String result = attacker.attack(target);
        nextTurn();
        notifyObservers(new GameEvent(GameEvent.Type.ATTACK, result));
        return result;
    }

    public String useAbility(String abilityName, Character target) {
        Character user = getActivePlayer();
        requireParticipant(target);
        String result = user.useAbility(abilityName, target);
        nextTurn();
        notifyObservers(new GameEvent(GameEvent.Type.ABILITY_USED, result));
        return result;
    }

    public String defend() {
        String result = getActivePlayer().defend();
        nextTurn();
        notifyObservers(new GameEvent(GameEvent.Type.DEFENDED, result));
        return result;
    }

    public String heal(int amount) {
        String result = getActivePlayer().heal(amount);
        nextTurn();
        notifyObservers(new GameEvent(GameEvent.Type.HEALED, result));
        return result;
    }

    public boolean isOver() {
        return players.stream().filter(player -> player.getHealth() > 0).count() <= 1;
    }

    public Character getWinner() {
        if (!isOver()) {
            return null;
        }
        return players.stream().filter(player -> player.getHealth() > 0).findFirst().orElse(null);
    }

    private void nextTurn() {
        if (isOver()) {
            return;
        }
        for (int offset = 1; offset <= players.size(); offset++) {
            int nextIndex = (currentPlayerIndex + offset) % players.size();
            if (players.get(nextIndex).getHealth() > 0) {
                currentPlayerIndex = nextIndex;
                return;
            }
        }
    }

    private void requireOpponent(Character target, Character attacker) {
        requireParticipant(target);
        if (target == attacker) {
            throw new IllegalArgumentException("A player cannot attack themselves");
        }
    }

    private void requireParticipant(Character target) {
        Objects.requireNonNull(target, "Target must not be null");
        if (!players.contains(target)) {
            throw new IllegalArgumentException("Target is not a player in this game");
        }
        if (target.getHealth() <= 0) {
            throw new IllegalArgumentException("A defeated player cannot be targeted");
        }
    }

    private Character getActivePlayer() {
        ensureStarted();
        if (isOver()) {
            throw new IllegalStateException("The game is over");
        }
        return players.get(currentPlayerIndex);
    }

    private void ensureStarted() {
        if (!started) {
            throw new IllegalStateException("The game has not started");
        }
    }

    private void notifyObservers(GameEvent event) {
        observers.forEach(observer -> observer.onGameEvent(event));
    }
}

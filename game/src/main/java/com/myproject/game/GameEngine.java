package com.myproject.game;

import java.util.List;

/** Public game-session contract used by clients instead of a concrete session. */
public interface GameEngine {
    void addPlayer(Character player);

    void start();

    Character getCurrentPlayer();

    List<Character> getPlayers();

    String attack(Character target);

    String useAbility(String abilityName, Character target);

    String defend();

    String heal(int amount);

    boolean isOver();

    Character getWinner();
}
package com.myproject.game;

/** A special action that belongs to a character. */
@FunctionalInterface
public interface Ability {
    String use(Character user, Character target);
}
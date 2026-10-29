package com.myproject.game;

/** Supplies abilities to the application layer without exposing construction details. */
@FunctionalInterface
public interface AbilityProvider {
    Ability create(String type);
}
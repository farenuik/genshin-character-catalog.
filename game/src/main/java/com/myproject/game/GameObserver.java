package com.myproject.game;

/** Observer notified after a game session changes. */
@FunctionalInterface
public interface GameObserver {
    void onGameEvent(GameEvent event);
}

package com.myproject;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MainCliTest {
    @Test
    void resolvesTextModeFromCliArgs() {
        assertEquals(Main.Mode.TEXT, Main.resolveMode(new String[] {"--text"}));
    }

    @Test
    void resolvesCharactersModeFromCliArgs() {
        assertEquals(Main.Mode.CHARS, Main.resolveMode(new String[] {"--chars"}));
    }

    @Test
    void usesDefaultTextModeWhenNoOptionsProvided() {
        assertEquals(Main.Mode.TEXT, Main.resolveMode(new String[0]));
    }
}

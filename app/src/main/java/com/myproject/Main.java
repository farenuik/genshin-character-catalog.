package com.myproject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/** Entry point for the command-line and graphical application modes. */
public final class Main {
    public enum Mode { TEXT, CHARS, GUI, GAME }

    private Main() { }

    public static void main(String[] args) throws Exception {
        Mode mode = resolveMode(args);
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

        if (args.length == 0) {
            System.out.println(
                    "Select mode: [1] text (default) | [2] chars | [3] GUI catalog | [4] GUI game");
            String line = input.readLine();
            if (line != null && line.equals("2")) {
                mode = Mode.CHARS;
            } else if (line != null && line.equals("3")) {
                mode = Mode.GUI;
            } else if (line != null && line.equals("4")) {
                mode = Mode.GAME;
            }
        }

        if (mode == Mode.GUI) {
            SwingUtilities.invokeLater(() -> new CharacterCatalogWindow().setVisible(true));
            return;
        }

        if (mode == Mode.GAME) {
            SwingUtilities.invokeLater(Main::openGameWindow);
            return;
        }

        if (mode == Mode.TEXT) {
            new TextCli(input).run();
            return;
        }

        new GameCli(input).run();
    }

    static Mode resolveMode(String[] args) {
        if (args == null || args.length == 0) {
            return Mode.TEXT;
        }

        for (String argument : args) {
            if ("--text".equals(argument)) return Mode.TEXT;
            if ("--chars".equals(argument)) return Mode.CHARS;
            if ("--gui".equals(argument)) return Mode.GUI;
            if ("--game".equals(argument)) return Mode.GAME;
        }

        return Mode.TEXT;
    }

    private static void openGameWindow() {
        try {
            new GameWindow().setVisible(true);
        } catch (java.io.IOException exception) {
            System.err.println("Could not open game window: " + exception.getMessage());
            JOptionPane.showMessageDialog(
                    null,
                    "Не вдалося завантажити локальний каталог персонажів: " + exception.getMessage(),
                    "Помилка запуску гри",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
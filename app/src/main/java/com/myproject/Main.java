package com.myproject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Entry point for the interactive command-line interface. */
public final class Main {
    public enum Mode { TEXT, CHARS }

    private Main() { }

    public static void main(String[] args) throws Exception {
        Mode mode = resolveMode(args);
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

        if (args.length == 0) {
            System.out.println("Select mode: [1] text (default) | [2] chars");
            String line = input.readLine();
            if (line != null && line.equals("2")) {
                mode = Mode.CHARS;
            }
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
        }

        return Mode.TEXT;
    }
}
package com.myproject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class CommandRegistry {
    private final Map<String, Command> commands = new LinkedHashMap<>();

    public CommandRegistry register(String name, Command command) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Command name must not be blank");
        }
        commands.put(name, Objects.requireNonNull(command, "Command must not be null"));
        return this;
    }

    public boolean execute(String line) {
        List<String> tokens = tokenize(line);
        if (tokens.isEmpty()) return true;
        if (tokens.get(0).equals("exit") || tokens.get(0).equals("quit")) return false;
        Command command = commands.get(tokens.get(0));
        if (command == null) {
            System.out.println("Unknown command: " + tokens.get(0));
            return true;
        }
        try {
            command.execute(tokens.subList(1, tokens.size()));
        } catch (Exception exception) {
            System.out.println("Error: " + exception.getMessage());
        }
        return true;
    }

    public String help() { return String.join(", ", commands.keySet()) + ", exit"; }

    public static List<String> tokenize(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder token = new StringBuilder();
        char quote = 0;
        for (char character : line.toCharArray()) {
            if ((character == '\'' || character == '"') && (quote == 0 || quote == character)) {
                quote = quote == 0 ? character : 0;
            } else if (Character.isWhitespace(character) && quote == 0) {
                if (!token.isEmpty()) { result.add(token.toString()); token.setLength(0); }
            } else token.append(character);
        }
        if (quote != 0) throw new IllegalArgumentException("Unclosed quote");
        if (!token.isEmpty()) result.add(token.toString());
        return result;
    }
}
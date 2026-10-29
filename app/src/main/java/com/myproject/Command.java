package com.myproject;

import java.util.List;

@FunctionalInterface
public interface Command {
    void execute(List<String> arguments) throws Exception;
}
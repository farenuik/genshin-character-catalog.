package com.myproject.university;

public abstract class Person implements PersonModel {
    private final String name;

    protected Person(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name must not be blank");
        }
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract String getRole();

    public abstract String getDescription();
}

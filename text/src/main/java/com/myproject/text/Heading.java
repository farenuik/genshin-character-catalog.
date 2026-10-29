package com.myproject.text;

import java.util.Optional;

/** A heading with a level from one to six. */
public final class Heading extends AbstractTextElement {
    private final int level;
    private final String title;

    public Heading(int level, String title) {
        super("heading");
        if (level < 1 || level > 6 || title == null || title.isBlank()) {
            throw new IllegalArgumentException("Heading level must be 1..6 and title must not be blank");
        }
        this.level = level;
        this.title = title;
    }

    public int getLevel() { return level; }
    public String getTitle() { return title; }

    @Override
    public <R> R accept(TextElementVisitor<R> visitor) {
        return visitor.visitHeading(this);
    }

    @Override
    public String render() {
        return "#".repeat(level) + " " + title;
    }

    @Override
    public Optional<String> tableOfContentsEntry() {
        return Optional.of("  ".repeat(level - 1) + "- " + title);
    }
}
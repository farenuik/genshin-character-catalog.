package com.myproject.text;

import java.util.Objects;
import java.util.function.Consumer;

/** Coordinates the creation of a structured text document using a unified API. */
public final class StructuredTextDirector {
    private final StructuredTextBuilder builder;

    public StructuredTextDirector() {
        this(new DefaultTextElementFactory());
    }

    public StructuredTextDirector(TextElementFactory elementFactory) {
        this.builder = new StructuredTextBuilder(elementFactory);
    }

    public Text construct(Consumer<StructuredTextBuilder> definition) {
        Objects.requireNonNull(definition, "Document definition must not be null");
        return builder.build(definition);
    }

    public Text construct() {
        return builder.build();
    }
}

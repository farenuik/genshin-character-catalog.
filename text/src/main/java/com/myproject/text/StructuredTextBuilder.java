package com.myproject.text;

import java.util.Objects;
import java.util.function.Consumer;

/** Unified API for creating a structured text document. */
public final class StructuredTextBuilder {
    private final Text text = new Text();
    private final TextElementFactory elementFactory;

    public StructuredTextBuilder() {
        this(new DefaultTextElementFactory());
    }

    public StructuredTextBuilder(TextElementFactory elementFactory) {
        this.elementFactory = Objects.requireNonNull(elementFactory, "Element factory must not be null");
    }

    /** Creates a document by delegating its construction to this coordinator. */
    public Text build(Consumer<StructuredTextBuilder> definition) {
        Objects.requireNonNull(definition, "Document definition must not be null");
        clear();
        definition.accept(this);
        return build();
    }

    public StructuredTextBuilder heading(int level, String title) {
        text.add(elementFactory.createHeading(level, title));
        return this;
    }

    public StructuredTextBuilder paragraph(String content) {
        text.add(elementFactory.createParagraph(content));
        return this;
    }

    public StructuredTextBuilder link(String label, String address) {
        text.add(elementFactory.createLink(label, address));
        return this;
    }

    public StructuredTextBuilder image(String alternativeText, String source) {
        text.add(elementFactory.createImage(alternativeText, source));
        return this;
    }

    public StructuredTextBuilder element(TextElement element) {
        text.add(element);
        return this;
    }

    /** Starts a new document with the same builder instance. */
    public StructuredTextBuilder clear() {
        text.clear();
        return this;
    }

    /** Builds an independent document snapshot. */
    public Text build() {
        return new Text(text);
    }
}
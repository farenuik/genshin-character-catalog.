package com.myproject.text;

import java.util.Optional;

/** A renderable part of a text document. */
public interface TextElement {
    /** Returns the logical type of this element. */
    String getType();

    /** Dispatches this element to a Visitor, allowing type-specific document operations. */
    default <R> R accept(TextElementVisitor<R> visitor) {
        return visitor.visitTextElement(this);
    }

    /** Renders this element as text suitable for terminal output. */
    String render();

    /** Supplies a table-of-contents entry when this element is a heading. */
    default Optional<String> tableOfContentsEntry() {
        return Optional.empty();
    }
}
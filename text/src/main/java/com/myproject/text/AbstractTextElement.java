package com.myproject.text;

/** Base abstraction for all structured text elements. */
public abstract class AbstractTextElement implements TextElement {
    private final String type;

    protected AbstractTextElement(String type) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Element type must not be blank");
        }
        this.type = type;
    }

    @Override
    public final String getType() {
        return type;
    }
}

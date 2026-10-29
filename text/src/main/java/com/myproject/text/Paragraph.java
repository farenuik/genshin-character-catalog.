package com.myproject.text;

/** A regular block of prose. */
public final class Paragraph extends AbstractTextElement {
    private final String content;

    public Paragraph(String content) {
        super("paragraph");
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Content must not be blank");
        }
        this.content = content;
    }

    @Override
    public <R> R accept(TextElementVisitor<R> visitor) {
        return visitor.visitParagraph(this);
    }

    @Override
    public String render() {
        return content;
    }
}
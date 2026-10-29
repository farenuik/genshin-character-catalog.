package com.myproject.text;

/** A textual representation of an image resource. */
public final class Image extends AbstractTextElement {
    private final String alternativeText;
    private final String source;

    public Image(String alternativeText, String source) {
        super("image");
        this.alternativeText = alternativeText;
        this.source = source;
    }

    @Override
    public <R> R accept(TextElementVisitor<R> visitor) {
        return visitor.visitImage(this);
    }

    @Override
    public String render() {
        return "[Image: " + alternativeText + ", source=" + source + "]";
    }
}
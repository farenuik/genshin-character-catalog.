package com.myproject.text;

/** A labelled hyperlink included in a document. */
public final class Link extends AbstractTextElement {
    private final String label;
    private final String address;

    public Link(String label, String address) {
        super("link");
        this.label = label;
        this.address = address;
    }

    @Override
    public <R> R accept(TextElementVisitor<R> visitor) {
        return visitor.visitLink(this);
    }

    @Override
    public String render() { return label + " (" + address + ")"; }
}
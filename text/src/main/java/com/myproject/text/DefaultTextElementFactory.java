package com.myproject.text;

/** Creates the default Markdown-like family of structured text elements. */
public final class DefaultTextElementFactory implements TextElementFactory {
    @Override
    public TextElement createHeading(int level, String title) {
        return new Heading(level, title);
    }

    @Override
    public TextElement createParagraph(String content) {
        return new Paragraph(content);
    }

    @Override
    public TextElement createLink(String label, String address) {
        return new Link(label, address);
    }

    @Override
    public TextElement createImage(String alternativeText, String source) {
        return new Image(alternativeText, source);
    }
}
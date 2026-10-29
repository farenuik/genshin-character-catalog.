package com.myproject.text;

/** Creates a compatible family of structured text elements. */
public interface TextElementFactory {
    TextElement createHeading(int level, String title);

    TextElement createParagraph(String content);

    TextElement createLink(String label, String address);

    TextElement createImage(String alternativeText, String source);
}
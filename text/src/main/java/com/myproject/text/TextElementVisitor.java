package com.myproject.text;

/** Visitor for operations that need to distinguish structured text element types. */
public interface TextElementVisitor<R> {
    R visitTextElement(TextElement element);

    default R visitHeading(Heading heading) {
        return visitTextElement(heading);
    }

    default R visitParagraph(Paragraph paragraph) {
        return visitTextElement(paragraph);
    }

    default R visitLink(Link link) {
        return visitTextElement(link);
    }

    default R visitImage(Image image) {
        return visitTextElement(image);
    }
}

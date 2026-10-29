package com.myproject.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class StructuredTextDirectorTest {

    @Test
    void directorBuildsDocumentUsingUnifiedApi() {
        Text article = new StructuredTextDirector().construct(document -> document
                .heading(1, "Inheritance")
                .paragraph("Shared behavior is defined once in a base abstraction.")
                .link("OpenJDK", "https://openjdk.org"));

        assertEquals("# Inheritance", article.toString().split(System.lineSeparator())[0]);
        assertTrue(article.toString().contains("OpenJDK (https://openjdk.org)"));
    }

    @Test
    void directorUsesInjectedElementFamily() {
        TextElementFactory factory = new TextElementFactory() {
            @Override
            public TextElement createHeading(int level, String title) {
                return element("heading", "H" + level + ": " + title);
            }

            @Override
            public TextElement createParagraph(String content) {
                return element("paragraph", "P: " + content);
            }

            @Override
            public TextElement createLink(String label, String address) {
                return element("link", "A: " + label + " -> " + address);
            }

            @Override
            public TextElement createImage(String alternativeText, String source) {
                return element("image", "IMG: " + alternativeText + " -> " + source);
            }
        };

        Text document = new StructuredTextDirector(factory).construct(builder -> builder
                .heading(2, "Title")
                .paragraph("Body")
                .link("Site", "/site")
                .image("Diagram", "/diagram.png"));

        assertEquals(String.join(System.lineSeparator(),
                "H2: Title",
                "P: Body",
                "A: Site -> /site",
                "IMG: Diagram -> /diagram.png"), document.toString());
    }

    private static TextElement element(String type, String rendered) {
        return new TextElement() {
            @Override
            public String getType() {
                return type;
            }

            @Override
            public String render() {
                return rendered;
            }

            @Override
            public Optional<String> tableOfContentsEntry() {
                return Optional.empty();
            }
        };
    }
}

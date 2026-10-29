package com.myproject.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** Ordered document container that manages heterogeneous text elements. */
public final class Text implements TextDocument {
    private final List<TextElement> elements = new ArrayList<>();

    Text() { }

    Text(Text source) {
        elements.addAll(source.elements);
    }

    /** Appends an element to the end of the document. */
    public void add(TextElement element) { elements.add(requireElement(element)); }

    /** Removes all elements from the document. */
    public void clear() { elements.clear(); }

    /** Inserts an element before the element at the specified index. */
    public void insert(int index, TextElement element) {
        elements.add(index, requireElement(element));
    }

    /** Removes and returns the element at the specified index. */
    public TextElement remove(int index) { return elements.remove(index); }

    /** Moves one element to another position in the document. */
    public void move(int fromIndex, int toIndex) {
        TextElement element = elements.remove(fromIndex);
        elements.add(toIndex, element);
    }

    public List<TextElement> elements() {
        return Collections.unmodifiableList(elements);
    }

    /** Returns only headings, preserving their document order and hierarchy. */
    public String tableOfContents() {
        return elements.stream()
                .map(element -> element.accept(TABLE_OF_CONTENTS_VISITOR))
                .flatMap(java.util.Optional::stream)
                .collect(Collectors.joining(System.lineSeparator(), "", ""));
    }

    /** Renders every document element on its own line. */
    @Override
    public String toString() {
        return elements.stream().map(element -> element.accept(RENDER_VISITOR))
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private static final TextElementVisitor<String> RENDER_VISITOR = new TextElementVisitor<>() {
        @Override
        public String visitTextElement(TextElement element) {
            return element.render();
        }
    };

    private static final TextElementVisitor<Optional<String>> TABLE_OF_CONTENTS_VISITOR =
            new TextElementVisitor<>() {
                @Override
                public Optional<String> visitTextElement(TextElement element) {
                    return element.tableOfContentsEntry();
                }

                @Override
                public Optional<String> visitHeading(Heading heading) {
                    return heading.tableOfContentsEntry();
                }
            };

    private static TextElement requireElement(TextElement element) {
        if (element == null) throw new IllegalArgumentException("Text element must not be null");
        return element;
    }
}
package com.myproject.text;

import java.util.List;

/** Mutable document contract; storage and construction can be replaced independently. */
public interface TextDocument extends Iterable<TextElement> {
    void add(TextElement element);

    void clear();

    void insert(int index, TextElement element);

    TextElement remove(int index);

    void move(int fromIndex, int toIndex);

    List<TextElement> elements();

    String tableOfContents();

    /** Iterates through the document's elements in their current order. */
    @Override
    default java.util.Iterator<TextElement> iterator() {
        return elements().iterator();
    }
}
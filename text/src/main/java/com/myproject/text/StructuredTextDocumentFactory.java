package com.myproject.text;

/** Default factory backed by the structured text implementation. */
public final class StructuredTextDocumentFactory implements TextDocumentFactory {
    @Override
    public TextDocument create() {
        return new StructuredTextDirector().construct();
    }
}
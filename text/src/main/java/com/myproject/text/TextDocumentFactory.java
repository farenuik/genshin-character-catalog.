package com.myproject.text;

/** Creates document implementations for clients that should not depend on storage details. */
@FunctionalInterface
public interface TextDocumentFactory {
    TextDocument create();
}
package com.myproject;

import com.myproject.text.Heading;
import com.myproject.text.Image;
import com.myproject.text.Link;
import com.myproject.text.Paragraph;
import com.myproject.text.StructuredTextDocumentFactory;
import com.myproject.text.TextDocument;
import com.myproject.text.TextDocumentFactory;
import com.myproject.text.TextElement;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

public final class TextCli {
    private final BufferedReader input;
    private final TextDocument document;
    private int position;

    public TextCli(BufferedReader input) {
        this(input, new StructuredTextDocumentFactory());
    }

    public TextCli(BufferedReader input, TextDocumentFactory documentFactory) {
        this.input = input;
        this.document = documentFactory.create();
    }

    public void run() throws IOException {
        CommandRegistry commands = new CommandRegistry()
                .register("pwd", arguments -> pwd())
                .register("print", this::print)
                .register("add", this::add)
                .register("help", arguments -> System.out.println("pwd, print [--whole|--id], add <container|leaf> <type>"));
        System.out.println("Text mode. Commands: " + commands.help());
        while (true) {
            System.out.print("> ");
            String line = input.readLine();
            if (line == null || !commands.execute(line)) return;
        }
    }

    private void pwd() {
        StringBuilder path = new StringBuilder();
        for (int index = 0; index < document.elements().size(); index++) {
            TextElement element = document.elements().get(index);
            if (element.getType().equals("heading")) {
                path.append('/').append(((Heading) element).getTitle());
            }
        }
        System.out.println(path.length() == 0 ? "/" : path);
    }

    private void print(List<String> arguments) {
        if (arguments.contains("--whole")) {
            System.out.println(document);
            return;
        }
        if (document.elements().isEmpty()) {
            System.out.println("(empty)");
            return;
        }
        TextElement element = document.elements().get(Math.min(position, document.elements().size() - 1));
        if (arguments.contains("--id")) {
            System.out.println(position + ": " + element.render());
            return;
        }
        System.out.println(element.render());
    }

    private void add(List<String> arguments) throws IOException {
        if (arguments.isEmpty()) {
            throw new IllegalArgumentException("Usage: add <container|leaf> <type> [value...]");
        }

        if (arguments.size() >= 2) {
            String kind = arguments.get(0);
            String type = arguments.get(1);
            if (kind.equals("heading") || kind.equals("container") && type.equals("heading")) {
                int level = 1;
                String title;
                if (arguments.size() >= 3) {
                    title = arguments.get(2);
                } else {
                    System.out.print("Level: ");
                    level = Integer.parseInt(input.readLine());
                    System.out.print("Title: ");
                    title = input.readLine();
                }
                document.add(new Heading(level, title));
                position = document.elements().size() - 1;
                return;
            }

            if (kind.equals("paragraph") || kind.equals("leaf") && type.equals("paragraph")) {
                String content = arguments.size() >= 3 ? String.join(" ", arguments.subList(2, arguments.size())) : input.readLine();
                document.add(new Paragraph(content));
                position = document.elements().size() - 1;
                return;
            }

            if (kind.equals("link") || kind.equals("leaf") && type.equals("link")) {
                String label = arguments.size() >= 3 ? arguments.get(2) : input.readLine();
                String address = arguments.size() >= 4 ? arguments.get(3) : input.readLine();
                document.add(new Link(label, address));
                position = document.elements().size() - 1;
                return;
            }

            if (kind.equals("image") || kind.equals("leaf") && type.equals("image")) {
                String alt = arguments.size() >= 3 ? arguments.get(2) : input.readLine();
                String source = arguments.size() >= 4 ? arguments.get(3) : input.readLine();
                document.add(new Image(alt, source));
                position = document.elements().size() - 1;
                return;
            }
        }

        String kind = arguments.get(0);
        String type = arguments.size() >= 2 ? arguments.get(1) : "";
        if (kind.equals("container") && type.equals("heading")) {
            System.out.print("Level: ");
            int level = Integer.parseInt(input.readLine());
            System.out.print("Title: ");
            document.add(new Heading(level, input.readLine()));
        } else if (kind.equals("leaf") && type.equals("paragraph")) {
            System.out.print("Content: ");
            document.add(new Paragraph(input.readLine()));
        } else if (kind.equals("leaf") && type.equals("link")) {
            System.out.print("Label: ");
            String label = input.readLine();
            System.out.print("Address: ");
            document.add(new Link(label, input.readLine()));
        } else if (kind.equals("leaf") && type.equals("image")) {
            System.out.print("Alternative text: ");
            String alternative = input.readLine();
            System.out.print("Source: ");
            document.add(new Image(alternative, input.readLine()));
        } else {
            throw new IllegalArgumentException("Usage: add <heading|paragraph|link|image> [values]");
        }
        position = document.elements().size() - 1;
    }
}
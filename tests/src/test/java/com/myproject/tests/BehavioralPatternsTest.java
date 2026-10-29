package com.myproject.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.myproject.game.Character;
import com.myproject.game.Game;
import com.myproject.game.GameEvent;
import com.myproject.game.GameMemento;
import com.myproject.game.GameObserver;
import com.myproject.text.Heading;
import com.myproject.text.StructuredTextBuilder;
import com.myproject.text.Text;
import com.myproject.text.TextElement;
import com.myproject.text.TextElementVisitor;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class BehavioralPatternsTest {

    @Test
    void visitorDispatchesByElementTypeWithoutChangingRendering() {
        Text document = new StructuredTextBuilder()
                .build(builder -> builder.heading(2, "Title").paragraph("Body"));
        TextElementVisitor<String> visitor = new TextElementVisitor<>() {
            @Override
            public String visitTextElement(TextElement element) {
                return "element";
            }

            @Override
            public String visitHeading(Heading heading) {
                return "heading:" + heading.getTitle();
            }
        };

        assertEquals("heading:Title", document.elements().get(0).accept(visitor));
        assertEquals("element", document.elements().get(1).accept(visitor));
        assertEquals(String.join(System.lineSeparator(), "## Title", "Body"), document.toString());
    }

    @Test
    void documentIteratorPreservesElementOrder() {
        Text document = new StructuredTextBuilder()
                .build(builder -> builder.heading(1, "First").paragraph("Second"));

        List<String> rendered = new ArrayList<>();
        for (var element : document) {
            rendered.add(element.render());
        }

        assertEquals(List.of("# First", "Second"), rendered);
    }

    @Test
    void observersReceiveSessionEventsAndCanUnsubscribe() {
        Game game = new Game();
        List<GameEvent> events = new ArrayList<>();
        GameObserver observer = events::add;
        game.addObserver(observer);
        Character first = new Character("First", 30, 0, 10);
        Character second = new Character("Second", 30, 0, 10);

        game.addPlayer(first);
        game.addPlayer(second);
        game.start();
        String attackResult = game.attack(second);

        assertEquals(List.of(
                GameEvent.Type.PLAYER_ADDED,
                GameEvent.Type.PLAYER_ADDED,
                GameEvent.Type.STARTED,
                GameEvent.Type.ATTACK), events.stream().map(GameEvent::type).toList());
        assertEquals(attackResult, events.get(3).description());

        game.removeObserver(observer);
        int eventCount = events.size();
        game.defend();
        assertEquals(eventCount, events.size());
    }

    @Test
    void mementoRestoresPlayerStatsTurnAndRoster() {
        Game game = new Game();
        Character first = new Character("First", 30, 0, 10);
        Character second = new Character("Second", 30, 0, 10);
        game.addPlayer(first);
        game.addPlayer(second);
        game.start();
        GameMemento memento = game.saveState();

        game.attack(second);
        assertEquals(20, second.getHealth());
        assertSame(second, game.getCurrentPlayer());

        game.restoreState(memento);
        assertEquals(30, second.getHealth());
        assertSame(first, game.getCurrentPlayer());
        assertTrue(game.getPlayers().contains(second));
    }
}

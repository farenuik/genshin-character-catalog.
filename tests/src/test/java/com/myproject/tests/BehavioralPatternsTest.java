package com.myproject.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @Test
    void attacksApplyArmorAndHalveTheNextBasicAttackAfterDefending() {
        Game game = new Game();
        Character attacker = new Character("Attacker", 100, 0, 20);
        Character defender = new Character("Defender", 100, 7, 10);
        game.addPlayer(attacker);
        game.addPlayer(defender);
        game.start();

        assertEquals("Attacker attacks Defender for 13 damage.", game.attack(defender));
        assertEquals(87, defender.getHealth());
        assertEquals("Defender braces for the next attack.", game.defend());
        assertEquals("Attacker attacks Defender for 6 damage.", game.attack(defender));
        assertEquals(81, defender.getHealth());
        assertFalse(defender.isDefending());
    }

    @Test
    void minimumDamageIsOneAndHealingIsCappedAtMaximumHealth() {
        Game game = new Game();
        Character weak = new Character("Weak", 50, 0, 0);
        Character armored = new Character("Armored", 50, 100, 0);
        game.addPlayer(weak);
        game.addPlayer(armored);
        game.start();

        game.attack(armored);
        assertEquals(49, armored.getHealth());
        game.defend();
        game.attack(armored);
        assertEquals(48, armored.getHealth());

        game.heal(18);
        assertEquals(50, armored.getHealth());
        assertSame(weak, game.getCurrentPlayer());
    }

    @Test
    void defeatedPlayersAreSkippedWhenSelectingTheNextTurn() {
        Game game = new Game();
        Character first = new Character("First", 100, 0, 10);
        Character defeated = new Character("Defeated", 5, 0, 1);
        Character third = new Character("Third", 100, 0, 10);
        game.addPlayer(first);
        game.addPlayer(defeated);
        game.addPlayer(third);
        game.start();

        game.attack(defeated);
        assertEquals(0, defeated.getHealth());
        assertSame(third, game.getCurrentPlayer());
        assertFalse(game.isOver());
    }

    @Test
    void finishedGamesRejectFurtherActions() {
        Game game = new Game();
        Character winner = new Character("Winner", 10, 0, 10);
        Character loser = new Character("Loser", 10, 0, 10);
        game.addPlayer(winner);
        game.addPlayer(loser);
        game.start();

        game.attack(loser);

        assertTrue(game.isOver());
        assertSame(winner, game.getWinner());
        assertThrows(IllegalStateException.class, game::defend);
    }

    @Test
    void rejectsAddingTheSameCharacterMoreThanOnce() {
        Game game = new Game();
        Character player = new Character("Player", 100, 0, 10);
        game.addPlayer(player);

        assertThrows(IllegalArgumentException.class, () -> game.addPlayer(player));
        assertEquals(1, game.getPlayers().size());
    }
}

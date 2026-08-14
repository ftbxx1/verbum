package dev.verbum;

import dev.verbum.ast.EventHandler;
import dev.verbum.ast.Program;
import dev.verbum.lex.Line;
import dev.verbum.lex.Tokenizer;
import dev.verbum.parser.Parser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ParserTest {

    private Program parse(String src) {
        List<Line> lines = new Tokenizer(src, "test").tokenize();
        return new Parser(lines).parse();
    }

    @Test
    void parsesEventsActionsAndBlocks() {
        String src = """
            when player joins
                tell player hello
                kill player
            when boss dies
                give all players 1 dragon egg
            """;
        Program p = parse(src);
        assertEquals(2, p.events().size());
        EventHandler first = p.events().get(0);
        assertEquals(EventHandler.Kind.WHEN, first.kind());
        assertEquals(2, first.body().statements().size());
    }

    @Test
    void parsesCustomActions() {
        String src = """
            action reward player
                give player 10 diamonds
            when player completes quest
                reward player
            """;
        Program p = parse(src);
        assertEquals(1, p.actions().size());
        assertEquals("reward", p.actions().get(0).name());
        assertEquals(1, p.actions().get(0).parameters().size());
        assertEquals(1, p.events().size());
    }

    @Test
    void parsesEveryAndOn() {
        String src = """
            every 10 seconds
                heal all players
            on server start
                announce up
            """;
        Program p = parse(src);
        EventHandler e1 = p.events().get(0);
        assertEquals(EventHandler.Kind.EVERY, e1.kind());
        assertEquals(Integer.valueOf(10), e1.numberSeconds());
        EventHandler e2 = p.events().get(1);
        assertEquals(EventHandler.Kind.ON, e2.kind());
        assertEquals("server start", String.join(" ", e2.trigger()));
    }

    @Test
    void parsesIfElseAndLoops() {
        String src = """
            every 5 seconds
                if player has 5 diamonds
                    give player diamond
                else if player has 3 diamonds
                    give player emerald
                else
                    give player stick
                repeat 3 times
                    spawn zombie
            """;
        Program p = parse(src);
        assertEquals(1, p.events().size());
        assertEquals(2, p.events().get(0).body().statements().size()); // if + repeat
    }
}

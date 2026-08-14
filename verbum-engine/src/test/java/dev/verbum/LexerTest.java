package dev.verbum;

import dev.verbum.lex.Line;
import dev.verbum.lex.Tokenizer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LexerTest {

    @Test
    void tokenizesWordsNumbersAndIndentation() {
        String src = "when player touches water\n    kill player\ngive 5,000 diamonds\n";
        List<Line> lines = new Tokenizer(src, "t").tokenize();
        assertEquals(3, lines.size());
        assertEquals(0, lines.get(0).indent());
        assertEquals(4, lines.get(1).indent());
        assertEquals(0, lines.get(2).indent());

        // "5,000" should become one number token
        Line l = lines.get(2);
        assertEquals(3, l.tokens().size());
        assertEquals(5000, l.tokens().get(1).number().longValue());
    }

    @Test
    void skipsComments() {
        String src = "note this is a comment\n# another one\nwhen player joins\n";
        List<Line> lines = new Tokenizer(src, "t").tokenize();
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).tokens().get(0).is("when"));
    }

    @Test
    void ignoresBlankLinesAndInlineComments() {
        String src = "\nwhen player joins # hey\n\n    kill player\n";
        List<Line> lines = new Tokenizer(src, "t").tokenize();
        assertEquals(2, lines.size());
    }

    @Test
    void treatsUnknownAsWord() {
        String src = "give player 1o0 diamonds\n";
        // "1o0" is not a number -> treated as a single word
        List<Line> lines = new Tokenizer(src, "t").tokenize();
        assertEquals(4, lines.get(0).tokens().size());
    }
}

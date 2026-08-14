package dev.verbum.lex;

import java.util.List;

/**
 * One source line of Verbum code, split into tokens and carrying its indentation.
 *
 * Indentation (leading spaces) defines blocks. A deeper indent means the line
 * belongs inside the block started by the previous, shallower line.
 */
public final class Line {

    private final int lineNumber;
    private final int indent;
    private final List<Token> tokens;

    public Line(int lineNumber, int indent, List<Token> tokens) {
        this.lineNumber = lineNumber;
        this.indent = indent;
        this.tokens = tokens;
    }

    /** 1-based line number in the original file. */
    public int lineNumber() { return lineNumber; }

    /** Number of leading spaces. */
    public int indent() { return indent; }

    public List<Token> tokens() { return tokens; }

    public boolean isEmpty() { return tokens.isEmpty(); }

    /** The text of all tokens joined with spaces (used for free-form messages/names). */
    public String text() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tokens.size(); i++) {
            if (i > 0) sb.append(' ');
            sb.append(tokens.get(i).text());
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return lineNumber + ":" + " ".repeat(indent) + text();
    }
}

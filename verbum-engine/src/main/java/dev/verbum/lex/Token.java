package dev.verbum.lex;

import java.math.BigDecimal;

/**
 * A single word of Verbum code.
 *
 * Verbum has no punctuation, so every meaningful unit of a line is either a
 * plain word (example: "give", "player", "diamond") or a number ("5", "1,000", "1.5").
 */
public final class Token {

    public enum Type { WORD, NUMBER }

    private final Type type;
    private final String text;
    private final BigDecimal number;

    private Token(Type type, String text, BigDecimal number) {
        this.type = type;
        this.text = text;
        this.number = number;
    }

    public static Token word(String text) {
        return new Token(Type.WORD, text, null);
    }

    public static Token number(BigDecimal value) {
        return new Token(Type.NUMBER, value.stripTrailingZeros().toPlainString(), value);
    }

    /** True if the lower-cased text equals the given word. */
    public boolean is(String word) {
        return type == Type.WORD && text.equalsIgnoreCase(word);
    }

    /** True if the text (ignoring case) is one of the given words. */
    public boolean isAny(String... words) {
        for (String w : words) {
            if (is(w)) return true;
        }
        return false;
    }

    public Type type() { return type; }
    public String text() { return text; }
    public BigDecimal number() { return number; }

    @Override
    public String toString() {
        return type + "(" + text + ")";
    }
}

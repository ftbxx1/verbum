package dev.verbum.ast;

import java.util.List;

/** repeat N times ... */
public final class RepeatTimes implements Stmt {

    private final int line;
    private final List<String> countWords;   // e.g. ["10"] or a variable name
    private final Block body;

    public RepeatTimes(int line, List<String> countWords, Block body) {
        this.line = line;
        this.countWords = countWords;
        this.body = body;
    }

    @Override public int line() { return line; }
    public List<String> countWords() { return countWords; }
    public Block body() { return body; }
}

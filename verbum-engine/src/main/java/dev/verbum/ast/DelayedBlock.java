package dev.verbum.ast;

import java.util.List;

/**
 * Runs a block later, asynchronously relative to the current script:
 *   after 5 seconds
 *       tell player done waiting
 *
 * The block is scheduled and the script continues running immediately
 * afterwards (unlike  wait, which pauses the current script).
 */
public final class DelayedBlock implements Stmt {

    private final int line;
    private final List<String> delayWords;   // e.g. ["5", "seconds"]
    private final Block body;

    public DelayedBlock(int line, List<String> delayWords, Block body) {
        this.line = line;
        this.delayWords = delayWords;
        this.body = body;
    }

    @Override public int line() { return line; }
    public List<String> delayWords() { return delayWords; }
    public Block body() { return body; }
}
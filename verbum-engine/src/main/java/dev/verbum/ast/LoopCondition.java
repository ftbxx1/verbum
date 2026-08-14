package dev.verbum.ast;

import java.util.List;

/** repeat while COND ...  and  until COND ...  (both loop with a condition) */
public final class LoopCondition implements Stmt {

    public enum Mode { WHILE, UNTIL }

    private final int line;
    private final Mode mode;
    private final List<String> condition;
    private final Block body;

    public LoopCondition(int line, Mode mode, List<String> condition, Block body) {
        this.line = line;
        this.mode = mode;
        this.condition = condition;
        this.body = body;
    }

    @Override public int line() { return line; }
    public Mode mode() { return mode; }
    public List<String> condition() { return condition; }
    public Block body() { return body; }
}

package dev.verbum.ast;

import java.util.List;

/**
 * A call to an action (built-in or custom), or a variable change.
 *
 * The first word of the line is the "verb" (for example: give, tell, kill,
 * teleport, set, add, remove, open). Everything after it is kept as raw words
 * and interpreted at runtime, so Verbum can accept many natural phrasings.
 */
public final class ActionCall implements Stmt {

    private final int line;
    private final String verb;
    private final List<String> args;

    public ActionCall(int line, String verb, List<String> args) {
        this.line = line;
        this.verb = verb;
        this.args = args;
    }

    @Override public int line() { return line; }
    public String verb() { return verb; }
    public List<String> args() { return args; }

    @Override
    public String toString() {
        return "action " + verb + " " + String.join(" ", args);
    }
}

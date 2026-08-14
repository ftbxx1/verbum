package dev.verbum.ast;

import java.util.List;

/**
 * A custom action (function):
 *   action reward player
 *       give player 10 diamonds
 *       tell player You received a reward
 */
public final class CustomAction implements Stmt {

    private final int line;
    private final String name;
    private final List<String> parameters;
    private final Block body;

    public CustomAction(int line, String name, List<String> parameters, Block body) {
        this.line = line;
        this.name = name;
        this.parameters = parameters;
        this.body = body;
    }

    @Override public int line() { return line; }
    public String name() { return name; }
    public List<String> parameters() { return parameters; }
    public Block body() { return body; }
}

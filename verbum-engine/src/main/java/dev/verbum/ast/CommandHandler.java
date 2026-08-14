package dev.verbum.ast;

import java.util.List;

/**
 * A user-defined command:
 *
 *   command greet name
 *       tell player Hello name
 *
 * The first word is the command name (so "/greet name" runs it), the words
 * after it are the argument names that get filled in when the command is run.
 */
public final class CommandHandler {

    private final int line;
    private final String name;
    private final List<String> parameters;
    private final Block body;

    public CommandHandler(int line, String name, List<String> parameters, Block body) {
        this.line = line;
        this.name = name;
        this.parameters = parameters;
        this.body = body;
    }

    public int line() { return line; }
    public String name() { return name; }
    public List<String> parameters() { return parameters; }
    public Block body() { return body; }
}

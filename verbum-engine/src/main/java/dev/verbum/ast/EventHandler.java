package dev.verbum.ast;

import java.util.List;

/**
 * An event handler:
 *   every 5 seconds            -> kind=EVERY,  number=5
 *   on server start            -> kind=ON,     trigger="server start"
 *   when player joins          -> kind=WHEN,   condition = words
 *
 * WHEN handlers run in priority order: high (1) before normal (0) before
 * low (-1). Example:  when player joins priority high
 */
public final class EventHandler implements Stmt {

    public enum Kind { WHEN, EVERY, ON }

    private final int line;
    private final Kind kind;
    private final Integer numberSeconds;   // for EVERY
    private final List<String> trigger;    // for ON (e.g. "server start")
    private final List<String> condition;  // for WHEN (raw words)
    private final int priority;            // -1 low, 0 normal, +1 high
    private final Block body;

    public EventHandler(int line, Kind kind, Integer numberSeconds,
                        List<String> trigger, List<String> condition, Block body) {
        this(line, kind, numberSeconds, trigger, condition, 0, body);
    }

    public EventHandler(int line, Kind kind, Integer numberSeconds,
                        List<String> trigger, List<String> condition, int priority, Block body) {
        this.line = line;
        this.kind = kind;
        this.numberSeconds = numberSeconds;
        this.trigger = trigger;
        this.condition = condition;
        this.priority = priority;
        this.body = body;
    }

    @Override public int line() { return line; }
    public Kind kind() { return kind; }
    public Integer numberSeconds() { return numberSeconds; }
    public List<String> trigger() { return trigger; }
    public List<String> condition() { return condition; }
    public int priority() { return priority; }
    public Block body() { return body; }
}
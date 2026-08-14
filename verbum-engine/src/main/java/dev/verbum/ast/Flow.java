package dev.verbum.ast;

/** break / stop / continue — loop control words. */
public final class Flow implements Stmt {

    public enum Kind { BREAK, CONTINUE, STOP }

    private final int line;
    private final Kind kind;

    public Flow(int line, Kind kind) {
        this.line = line;
        this.kind = kind;
    }

    @Override public int line() { return line; }
    public Kind kind() { return kind; }
}

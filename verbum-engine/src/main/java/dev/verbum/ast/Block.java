package dev.verbum.ast;

import java.util.ArrayList;
import java.util.List;

/** An ordered list of statements — the body of an event, action, if-branch or loop. */
public final class Block {

    private final List<Stmt> statements = new ArrayList<>();

    public void add(Stmt s) { statements.add(s); }

    public List<Stmt> statements() { return statements; }

    public boolean isEmpty() { return statements.isEmpty(); }
}

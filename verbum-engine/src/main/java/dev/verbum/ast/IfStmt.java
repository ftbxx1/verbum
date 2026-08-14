package dev.verbum.ast;

import java.util.List;

/**
 * An if / else if / else statement. Each branch carries its condition words and body.
 * The final else-body (no condition) is optional.
 */
public final class IfStmt implements Stmt {

    private final int line;
    private final List<List<String>> conditions;   // each branch's condition words
    private final List<Block> bodies;              // matching body per branch
    private final Block elseBody;                  // may be empty

    public IfStmt(int line, List<List<String>> conditions, List<Block> bodies, Block elseBody) {
        this.line = line;
        this.conditions = conditions;
        this.bodies = bodies;
        this.elseBody = elseBody;
    }

    @Override public int line() { return line; }
    public List<List<String>> conditions() { return conditions; }
    public List<Block> bodies() { return bodies; }
    public Block elseBody() { return elseBody; }
}

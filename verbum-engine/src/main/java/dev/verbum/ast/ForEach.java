package dev.verbum.ast;

import java.util.List;

/** for each NAME in LIST ... */
public final class ForEach implements Stmt {

    private final int line;
    private final String itemName;        // loop variable name
    private final List<String> listWords; // what to iterate (e.g. "inventory", "online players")
    private final Block body;

    public ForEach(int line, String itemName, List<String> listWords, Block body) {
        this.line = line;
        this.itemName = itemName;
        this.listWords = listWords;
        this.body = body;
    }

    @Override public int line() { return line; }
    public String itemName() { return itemName; }
    public List<String> listWords() { return listWords; }
    public Block body() { return body; }
}

package dev.verbum.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * A custom clickable menu (GUI):
 *
 *   menu rewards
 *       add button Unlock Sword
 *           give player diamond sword
 *       add button Buy Bread
 *           take 5 coins from player
 *           give player bread
 *
 * Opening the menu shows the buttons; clicking one runs its small block.
 */
public final class MenuBlock {

    public final static class Button {
        public final String label;
        public final int line;
        public final Block body;
        public Button(String label, int line, Block body) {
            this.label = label;
            this.line = line;
            this.body = body;
        }
    }

    private final int line;
    private final String name;
    private final List<Button> buttons = new ArrayList<>();

    public MenuBlock(int line, String name) {
        this.line = line;
        this.name = name;
    }

    public int line() { return line; }
    public String name() { return name; }
    public List<Button> buttons() { return buttons; }
    public void addButton(Button b) { buttons.add(b); }
}

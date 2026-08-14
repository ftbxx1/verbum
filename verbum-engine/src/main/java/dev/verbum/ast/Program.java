package dev.verbum.ast;

import java.util.ArrayList;
import java.util.List;

/** The root of a compiled Verbum script: a list of events and custom actions. */
public final class Program {

    private final List<EventHandler> events = new ArrayList<>();
    private final List<CustomAction> actions = new ArrayList<>();
    private final List<CommandHandler> commands = new ArrayList<>();
    private final List<MenuBlock> menus = new ArrayList<>();

    public void addEvent(EventHandler e) { events.add(e); }
    public void addAction(CustomAction a) { actions.add(a); }
    public void addCommand(CommandHandler c) { commands.add(c); }
    public void addMenu(MenuBlock m) { menus.add(m); }

    public List<EventHandler> events() { return events; }
    public List<CustomAction> actions() { return actions; }
    public List<CommandHandler> commands() { return commands; }
    public List<MenuBlock> menus() { return menus; }
}

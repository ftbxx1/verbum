package dev.verbum.paper;

import dev.verbum.engine.ScriptEngine;
import dev.verbum.error.VerbumError;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

/**
 * A command defined in a .vb file (e.g. "command hello"). Paper registers one of
 * these per script command so players can simply type /hello.
 */
public final class VerbumCommand extends Command {

    private final VerbumPaperPlugin plugin;
    private final String cmdName;

    public VerbumCommand(VerbumPaperPlugin plugin, String cmdName) {
        super(cmdName);
        this.plugin = plugin;
        this.cmdName = cmdName;
        this.setUsage("/" + cmdName + " <arguments>");
        this.setDescription("A Verbum custom command.");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        ScriptEngine engine = plugin.engine();
        if (engine == null) return true;
        String player = (sender instanceof Player p) ? p.getName() : "console";
        try {
            engine.interpreter().runCommand(cmdName, Arrays.asList(args), player);
        } catch (VerbumError e) {
            sender.sendMessage("[Verbum] " + e.getMessage());
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        return java.util.Collections.emptyList();
    }
}

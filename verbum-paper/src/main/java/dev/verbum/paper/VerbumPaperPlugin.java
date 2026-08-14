package dev.verbum.paper;

import dev.verbum.engine.ScriptEngine;
import dev.verbum.error.VerbumError;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.nio.file.Files;
import java.util.logging.Level;

/**
 * The Paper plugin. It loads every .vb file from plugins/Verbum/scripts/ into a
 * Verbum engine, bridges Minecraft events into the language, and runs actions
 * back into the world through PaperRuntime.
 */
public final class VerbumPaperPlugin extends JavaPlugin {

    private ScriptEngine engine;
    private PaperRuntime runtime;
    private BukkitTask tickTask;
    private VerbumGui gui;

    @Override
    public void onEnable() {
        File scripts = new File(getDataFolder(), "scripts");
        if (!scripts.exists()) {
            scripts.mkdirs();
            saveResource("scripts/game.mcscript", false);
        }

        runtime = new PaperRuntime(this);
        engine = new ScriptEngine(runtime);
        gui = new VerbumGui(this);

        registerBridge();
        getServer().getPluginManager().registerEvents(gui, this);
        reloadScripts();
        registerCommands();

        // Drive "every N seconds", condition handlers, deferred waits and GUI refreshes.
        tickTask = getServer().getScheduler().runTaskTimer(this, () -> {
            try {
                engine.tick();
                gui.openPendingMenus();
            } catch (Exception e) { getLogger().log(Level.WARNING, "Verbum tick error", e); }
        }, 20L, 20L);

        getLogger().info("Verbum enabled.");
    }

    @Override
    public void onDisable() {
        if (tickTask != null) tickTask.cancel();
        if (engine != null) { try { engine.onServerStop(); } catch (Exception ignored) {} }
        getLogger().info("Verbum disabled.");
    }

    private void registerBridge() {
        getServer().getPluginManager().registerEvents(new VerbumListener(this, engine, runtime), this);
    }

    public void reloadScripts() {
        File scripts = new File(getDataFolder(), "scripts");
        engine = new ScriptEngine(runtime);
        File[] files = scripts.listFiles((d, n) -> n.endsWith(".vb") || n.endsWith(".mcscript"));
        if (files == null) return;
        int loaded = 0;
        for (File f : files) {
            try {
                engine.loadFile(f.getAbsolutePath());
                loaded++;
            } catch (VerbumError e) {
                getLogger().warning("Verbum problem in " + f.getName() + ":\n" + e.pretty());
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Could not load " + f.getName(), e);
            }
        }
        getLogger().info("Loaded " + loaded + " Verbum script(s).");
        try { engine.onServerStart(); } catch (Exception e) { getLogger().log(Level.WARNING, "server start", e); }
        registerCommands();
    }

    /** Registers every custom command from the scripts so /hello etc. work. */
    public void registerCommands() {
        if (engine == null) return;
        try {
            var server = getServer();
            java.lang.reflect.Method m = server.getClass().getMethod("getCommandMap");
            Object cm = m.invoke(server);
            java.lang.reflect.Method register = cm.getClass().getMethod("register", String.class, org.bukkit.command.Command.class);
            for (String name : engine.interpreter().commandNames()) {
                register.invoke(cm, getName(), new VerbumCommand(this, name));
            }
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Could not register Verbum commands", e);
        }
    }

    public ScriptEngine engine() { return engine; }
    public PaperRuntime runtime() { return runtime; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("verbum") && args.length > 0
                && args[0].equalsIgnoreCase("reload")) {
            reloadScripts();
            sender.sendMessage("[Verbum] Scripts reloaded.");
            return true;
        }
        return false;
    }
}

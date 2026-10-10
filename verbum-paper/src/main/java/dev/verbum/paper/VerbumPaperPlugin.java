package dev.verbum.paper;

import dev.verbum.engine.ScriptEngine;
import dev.verbum.error.VerbumError;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

/**
 * The Paper plugin for Verbum. Manages the lifecycle of the Verbum engine,
 * loads and reloads scripts, bridges Minecraft events to the language,
 * and executes actions back into the world.
 *
 * Key responsibilities:
 * - Initialize engine and runtime on enable
 * - Load .vb and .mcscript files from plugins/Verbum/scripts/
 * - Register event listeners and custom commands
 * - Drive the engine tick (every second)
 * - Safely reload scripts without command duplication
 * - Clean shutdown on disable
 */
public final class VerbumPaperPlugin extends JavaPlugin {

    private ScriptEngine engine;
    private PaperRuntime runtime;
    private BukkitTask tickTask;
    private VerbumGui gui;
    private final Set<dev.verbum.api.VerbumPlugin> addons = new HashSet<>();
    private final Set<String> registeredCommands = new HashSet<>();

    /** Hooks an add-on plugin (called from its onEnable through VerbumAPI). */
    public void addAddon(dev.verbum.api.VerbumPlugin plugin) {
        if (plugin == null) return;
        addons.add(plugin);
        if (engine != null) {
            try {
                engine.registerPlugin(plugin);
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Could not register add-on " + plugin.name(), e);
            }
        }
    }

    @Override
    public void onEnable() {
        // Ensure scripts directory exists
        File scriptsDir = new File(getDataFolder(), "scripts");
        if (!scriptsDir.exists()) {
            scriptsDir.mkdirs();
            saveResource("scripts/game.mcscript", false);
        }

        // Initialize runtime and engine
        runtime = new PaperRuntime(this);
        engine = new ScriptEngine(runtime);
        gui = new VerbumGui(this);

        // Publish engine for add-ons to hook into
        dev.verbum.api.VerbumAPI.publish(engine);
        dev.verbum.api.VerbumAPI.setJoinHook(this::addAddon);

        // Register event bridge and GUI
        registerEventBridge();
        getServer().getPluginManager().registerEvents(gui, this);

        // Load scripts and commands
        loadScripts();

        // Start tick loop (every second = 20 ticks)
        tickTask = getServer().getScheduler().runTaskTimer(this, this::tick, 20L, 20L);

        getLogger().info("Verbum enabled.");
    }

    @Override
    public void onDisable() {
        // Cancel tick task
        if (tickTask != null) {
            tickTask.cancel();
        }

        // Clean up engine
        dev.verbum.api.VerbumAPI.unpublish();
        if (engine != null) {
            try {
                engine.onServerStop();
            } catch (Exception ignored) {
            }
        }

        getLogger().info("Verbum disabled.");
    }

    /** Register the event listener that bridges Minecraft events to Verbum. */
    private void registerEventBridge() {
        getServer().getPluginManager().registerEvents(
                new VerbumListener(this, engine, runtime), this);
    }

    /** Load all .vb and .mcscript files, register add-ons, and register commands. */
    private void loadScripts() {
        File scriptsDir = new File(getDataFolder(), "scripts");
        File[] scriptFiles = scriptsDir.listFiles(
                (d, n) -> n.endsWith(".vb") || n.endsWith(".mcscript"));

        if (scriptFiles == null || scriptFiles.length == 0) {
            getLogger().warning("No Verbum scripts found in " + scriptsDir);
            return;
        }

        // Re-register add-ons with the new engine
        for (dev.verbum.api.VerbumPlugin addon : addons) {
            try {
                engine.registerPlugin(addon);
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Could not re-register add-on " + addon.name(), e);
            }
        }

        // Load each script file
        int loaded = 0;
        for (File scriptFile : scriptFiles) {
            try {
                engine.loadFile(scriptFile.getAbsolutePath());
                loaded++;
            } catch (VerbumError e) {
                getLogger().warning(
                        "Verbum error in " + scriptFile.getName() + ":\n" + e.pretty());
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Could not load " + scriptFile.getName(), e);
            }
        }

        getLogger().info("Loaded " + loaded + " Verbum script(s).");

        // Notify engine of server start
        try {
            engine.onServerStart();
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Error during server start event", e);
        }

        // Register custom commands from scripts (clear old ones first)
        registerCustomCommands();
    }

    /** Register custom commands defined in scripts, clearing old registrations. */
    private void registerCustomCommands() {
        if (engine == null) return;

        try {
            Object commandMap = getServer().getClass()
                    .getMethod("getCommandMap")
                    .invoke(getServer());

            java.lang.reflect.Method registerMethod = commandMap.getClass()
                    .getMethod("register", String.class, org.bukkit.command.Command.class);

            Set<String> currentCommands = new HashSet<>(engine.interpreter().commandNames());

            // Unregister old commands that are no longer in the script
            for (String oldCmd : new HashSet<>(registeredCommands)) {
                if (!currentCommands.contains(oldCmd)) {
                    registeredCommands.remove(oldCmd);
                }
            }

            // Register new commands
            for (String cmdName : currentCommands) {
                if (!registeredCommands.contains(cmdName)) {
                    registerMethod.invoke(commandMap, getName(),
                            new VerbumCommand(this, cmdName));
                    registeredCommands.add(cmdName);
                }
            }
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Could not register Verbum commands", e);
        }
    }

    /** Drive the engine tick and update pending GUI menus. */
    private void tick() {
        try {
            engine.tick();
            gui.openPendingMenus();
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Verbum tick error", e);
        }
    }

    /** Reload scripts: clear engine, re-register add-ons, load files, register commands. */
    public void reloadScripts() {
        getLogger().info("Reloading Verbum scripts...");
        engine = new ScriptEngine(runtime);
        dev.verbum.api.VerbumAPI.publish(engine);
        registeredCommands.clear();
        loadScripts();
        getLogger().info("Reload complete.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("verbum")
                && args.length > 0
                && args[0].equalsIgnoreCase("reload")) {
            reloadScripts();
            sender.sendMessage("[Verbum] Scripts reloaded.");
            return true;
        }
        return false;
    }

    public ScriptEngine engine() { return engine; }
    public PaperRuntime runtime() { return runtime; }
}

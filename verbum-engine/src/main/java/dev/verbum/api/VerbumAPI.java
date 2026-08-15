package dev.verbum.api;

import dev.verbum.engine.ScriptEngine;

/**
 * The runtime door for the ecosystem. Any plugin (say an ExcellentCrates-style
 * add-on) can wire itself into the running Verbum engine without touching the
 * engine source:
 *
 * <pre>
 *   if (VerbumAPI.isAvailable()) {
 *       VerbumAPI api = VerbumAPI.get();
 *       api.registerPlugin(new MyCratesPlugin());
 *       api.registerAction(NativeAction.of("create crate", (it, words, line) -&gt; ...));
 *   }
 * </pre>
 *
 * The Paper plugin publishes the live engine here on enable and on every
 * {@code /verbum reload}, so add-ons always talk to the current engine. The
 * CLI/offline engine also publishes when it exists.
 */
public final class VerbumAPI {

    private static volatile ScriptEngine engine;
    private static volatile java.util.function.Consumer<VerbumPlugin> joinHook;

    private VerbumAPI() {}

    /** The public API handle. Never null; check {@link #isAvailable()} first. */
    public static VerbumAPI get() { return new VerbumAPI(); }

    /** Whether a Verbum engine is currently running (server plugin or CLI). */
    public static boolean isAvailable() { return engine != null; }

    /** Publishes the running engine (called by verbum-paper and the CLI). */
    public static synchronized void publish(ScriptEngine e) { engine = e; }

    /** Optional hook the host (paper plugin) installs to track joining plugins. */
    public static synchronized void setJoinHook(java.util.function.Consumer<VerbumPlugin> hook) { joinHook = hook; }

    /** Unpublishes on server stop. */
    public static synchronized void unpublish() { engine = null; joinHook = null; }

    /** The live engine, or null when none is running. */
    public ScriptEngine engine() { return engine; }

    /** Registers a plugin (name + vocabulary) into the live engine. */
    public void registerPlugin(VerbumPlugin plugin) {
        java.util.function.Consumer<VerbumPlugin> h = joinHook;
        if (h != null) h.accept(plugin);
        if (engine != null && plugin != null) engine.registerPlugin(plugin);
    }

    /** Registers a single native action verb into the live engine. */
    public void registerAction(NativeAction action) {
        if (engine != null) engine.registerAction(action);
    }

    /** Registers a single native function into the live engine. */
    public void registerFunction(NativeFunction function) {
        if (engine != null) engine.registerFunction(function);
    }

    /** Registers a native condition into the live engine. */
    public void registerCondition(NativeCondition condition) {
        if (engine != null) engine.registerCondition(condition);
    }

    /** Registers event vocabulary into the live engine. */
    public void registerEvent(EventWordMapper mapper) {
        if (engine != null) engine.registerEvent(mapper);
    }

    /** Names of all registered third-party plugins, for introspection. */
    public java.util.List<String> plugins() {
        return engine == null ? java.util.List.of() : engine.plugins();
    }
}
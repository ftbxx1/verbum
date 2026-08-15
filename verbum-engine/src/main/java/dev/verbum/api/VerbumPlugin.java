package dev.verbum.api;

/**
 * The extension point for the "ecosystem": any jar can implement this and hand
 * the engine new verbs, functions, conditions and event words without touching
 * Verbum's source.
 */
public interface VerbumPlugin {

    /** Human-readable plugin name, e.g. "ExcellentCrates" or "Vault". */
    String name();

    /** Plugin version, e.g. "5.3.2". */
    default String version() { return "1.0.0"; }

    /** Required Minecraft/Paper version, e.g. "1.20.6-R0.1-SNAPSHOT". */
    default String minecraftVersion() { return "1.20.6"; }

    /** Required Verbum engine version, e.g. "2.0.0". */
    default String verbumVersion() { return "1.0.0"; }

    /** Optional dependent plugins; each is checked at /verbum reload. */
    default java.util.List<String> dependencies() { return java.util.List.of(); }

    /** Called on every /verbum reload and on server start, after the engine is ready. */
    default void register(EngineRegistrar registrar) { }

    /** Default plugin handle for  plugin <name> ...  scripts, if this plugin exposes one. */
    default PluginBridge bridge() { return null; }

    /** Called when the engine is shutting down (persistence flush hook). */
    default void onDisable() { }
}
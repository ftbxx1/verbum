package dev.verbum.api;

/**
 * The extension point for the "ecosystem": any jar can implement this and hand
 * the engine new verbs, functions, conditions and event words without touching
 * the engine source.
 */
public interface VerbumPlugin {

    String name();

    /** Called on every /verbum reload and on server start, after the engine is ready. */
    default void register(EngineRegistrar registrar) { }

    /** Default plugin handle for  plugin <name> ...  scripts, if this plugin exposes one. */
    default PluginBridge bridge() { return null; }

    /** Called when the engine is shutting down (persistence flush hook). */
    default void onDisable() { }
}
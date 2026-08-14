package dev.verbum.api;

/**
 * The extension point for the "ecosystem": any jar can implement this and hand
 * the engine new verbs, functions, conditions and event words without touching
 * the engine source.
 */
public interface VerbumPlugin {

    String name();

    default void register(EngineRegistrar registrar) { }

    /** Called when the engine is shutting down (persistence flush hook). */
    default void onDisable() { }
}
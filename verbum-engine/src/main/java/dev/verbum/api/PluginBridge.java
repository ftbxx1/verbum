package dev.verbum.api;

import dev.verbum.interp.Interpreter;

import java.util.List;

/**
 * How a third-party plugin exposes its *whole* feature set to Verbum scripts
 * under one named keyword:
 *
 * <pre>
 *   plugin crates create crate KeyCrate at Spawn
 *   plugin crates does player have key
 * </pre>
 *
 * The first word after {@code plugin} is the plugin name (lower-cased), and the
 * rest is handed to the plugin to decide. This lets a plugin like ExcellentCrates
 * provide dozens of verbs/conditions without reserving any built-in word.
 */
public interface PluginBridge {

    /** The name scripts use:  plugin <name> ...   (matched case-insensitively). */
    String name();

    /** True when this plugin understands these words. */
    default boolean handles(List<String> words) { return true; }

    /** Runs a script action like  plugin <name> give key crate. */
    void action(Interpreter interp, List<String> words, int line);

    /** Answers a script condition like  plugin <name> does player have key. */
    default boolean condition(Interpreter interp, List<String> words) { return false; }
}
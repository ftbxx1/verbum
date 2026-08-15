package dev.verbum;

import dev.verbum.api.EngineRegistrar;
import dev.verbum.api.PluginBridge;
import dev.verbum.api.VerbumAPI;
import dev.verbum.api.VerbumPlugin;
import dev.verbum.engine.ScriptEngine;
import dev.verbum.runtime.MockMcRuntime;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The ecosystem in action: a third-party plugin (like an ExcellentCrates-style
 * add-on) wires itself through VerbumAPI and scripts then use its verbs,
 * functions, conditions and named bridge without any engine change.
 */
class EcosystemAddonTest {

    /** A fake third-party plugin exposing crate verbs/conditions/functions. */
    private static final class CratesAddon implements VerbumPlugin, PluginBridge {
        private final java.util.List<String> opened = new java.util.ArrayList<>();
        private final java.util.List<String> keys = new java.util.ArrayList<>();

        @Override public String name() { return "ExcellentCrates"; }

        @Override public void register(EngineRegistrar r) {
            r.registerAction(dev.verbum.api.NativeAction.of("create crate",
                    (it, words, line) -> {
                        String name = words.isEmpty() ? "KeyCrate" : words.get(0);
                        String who = it.focus();
                        it.runtime().announce("crate created " + name + " by " + who);
                    }));
            r.registerFunction(new dev.verbum.api.NativeFunction() {
                @Override public String name() { return "crate count"; }
                @Override public Object run(dev.verbum.interp.Interpreter it, List<String> words, int line) {
                    return (double) opened.size();
                }
            });
            r.registerCondition(new dev.verbum.api.NativeCondition() {
                @Override public boolean matches(List<String> words) {
                    return join(words).startsWith("key missing");
                }
                @Override public boolean eval(dev.verbum.interp.Interpreter it, List<String> words) {
                    return !keys.contains(it.focus());
                }
            });
        }

        @Override public PluginBridge bridge() { return this; }

        @Override public void action(dev.verbum.interp.Interpreter it, List<String> words, int line) {
            if (!words.isEmpty() && words.get(0).equalsIgnoreCase("open")) {
                String who = it.focus();
                opened.add(who);
                it.runtime().announce(who + " opened a crate");
            } else if (!words.isEmpty() && words.get(0).equalsIgnoreCase("give")) {
                keys.add(it.focus());
                it.runtime().announce("key given to " + it.focus());
            }
        }

        @Override public boolean condition(dev.verbum.interp.Interpreter it, List<String> words) {
            return !words.isEmpty() && words.get(0).equalsIgnoreCase("has") && keys.contains(it.focus());
        }

        private static String join(List<String> w) { return String.join(" ", w).toLowerCase(); }
    }

    @Test
    void multiwordNativeVerbsFiredStandalone() {
        MockMcRuntime runtime = new MockMcRuntime();
        ScriptEngine engine = new ScriptEngine(runtime);
        engine.registerPlugin(new CratesAddon());

        engine.load("""
            when the player joins
                create crate KeyCrate
            """, "addon.vb");
        engine.trigger("join", "Steve");
        engine.tick();

        assertTrue(runtime.chatter.stream().anyMatch(l -> l.contains("crate created KeyCrate")),
                "multi-word native verb 'create crate' must fire: " + runtime.chatter);
    }

    @Test
    void pluginNounPhraseRunsThroughBridge() {
        MockMcRuntime runtime = new MockMcRuntime();
        ScriptEngine engine = new ScriptEngine(runtime);
        engine.registerPlugin(new CratesAddon());

        engine.load("""
            when the player joins
                plugin ExcellentCrates give key
                if plugin ExcellentCrates has key
                    tell player You can open
            when the player joins
                plugin ExcellentCrates open menu
            """, "bridge.vb");
        engine.trigger("join", "Steve");
        engine.tick();

        assertTrue(runtime.chatter.stream().anyMatch(l -> l.contains("key given to Steve")),
                "bridge action must run: " + runtime.chatter);
        assertTrue(runtime.chatter.stream().anyMatch(l -> l.contains("Steve opened a crate")),
                "bridge action 'open' must run: " + runtime.chatter);
        assertTrue(runtime.chatter.stream().anyMatch(l -> l.contains("tell Steve: You can open")),
                "bridge condition 'plugin ExcellentCrates has key' must be true for Steve: " + runtime.chatter);
    }

    @Test
    void verbumApiPublishesEngineAndRoutesAddons() throws Exception {
        MockMcRuntime runtime = new MockMcRuntime();
        ScriptEngine engine = new ScriptEngine(runtime);
        VerbumAPI.publish(engine);

        // The add-on's own onEnable hook would call this.
        CratesAddon addon = new CratesAddon();
        VerbumAPI.get().registerPlugin(addon);

        assertTrue(VerbumAPI.isAvailable());
        assertEquals(List.of("ExcellentCrates"), VerbumAPI.get().plugins());

        engine.load("""
            when the player joins
                create crate Welcome
            """, "api.vb");
        engine.trigger("join", "Amy");
        engine.tick();

        assertTrue(runtime.chatter.stream().anyMatch(l -> l.contains("crate created Welcome")),
                "action registered through VerbumAPI must run: " + runtime.chatter);

        VerbumAPI.unpublish();
        assertFalse(VerbumAPI.isAvailable());
    }

    @Test
    void addonVocabularySurvivesReload() {
        MockMcRuntime runtime = new MockMcRuntime();
        ScriptEngine engine = new ScriptEngine(runtime);

        CratesAddon addon = new CratesAddon();
        // First engine gets the addon, then a wrapped "reload" re-registers it.
        engine.registerPlugin(addon);
        ScriptEngine reloaded = new ScriptEngine(runtime);
        reloaded.registerPlugin(addon);

        reloaded.load("""
            when the player joins
                create crate Again
            """, "reload.vb");
        reloaded.trigger("join", "Bob");
        reloaded.tick();

        assertTrue(runtime.chatter.stream().anyMatch(l -> l.contains("crate created Again")),
                "after /verbum reload the addon must still answer: " + runtime.chatter);
    }
}
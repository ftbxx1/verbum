package dev.verbum;

import dev.verbum.engine.ScriptEngine;
import dev.verbum.interp.Trigger;
import dev.verbum.runtime.MockMcRuntime;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The "100x more" expansion batch: event priorities + cancellation, a much
 * larger event vocabulary, the add-on API, async blocks, and persistence.
 */
class InterpreterExpansionTest {

    private static final class Scene {
        final MockMcRuntime runtime;
        final ScriptEngine engine;
        Scene(String src) {
            runtime = new MockMcRuntime();
            engine = new ScriptEngine(runtime);
            engine.load(src, "expansion.vb");
        }
    }

    // ------------------------------------------------------- events

    @Test
    void whenHandlersRunInPriorityOrder() {
        Scene s = new Scene("""
            when player joins priority high
                announce first
            when player joins
                announce middle
            when player joins priority low
                announce last
            """);
        s.engine.trigger("join", "Alex");
        assertEquals(List.of("announce: first", "announce: middle", "announce: last"), s.runtime.chatter);
    }

    @Test
    void cancelEventStopsLowerPriorityHandlers() {
        Scene s = new Scene("""
            when player joins priority high
                cancel event
                announce high-ran
            when player joins
                announce should-not-run
            """);
        s.engine.trigger("join", "Alex");
        assertEquals(List.of("announce: high-ran"), s.runtime.chatter);
    }

    @Test
    void cancelledFlagInsideCancellingHandler() {
        Scene s = new Scene("""
            when player joins
                cancel event
                if event is cancelled
                    announce cancelled-seen
            """);
        s.engine.trigger("join", "Alex");
        assertEquals(List.of("announce: cancelled-seen"), s.runtime.chatter);
    }

    @Test
    void manyNewEventKindsFire() {
        Scene s = new Scene("""
            when player quits
                announce quit-fires
            when player says hello
                announce chat-fires
            when player teleports to Spawn
                announce tp-fires
            when player starts swimming
                announce swim-fires
            when player catches fire
                announce burn-fires
            when player picks up a diamond
                announce pickup-fires
            when player opens a chest
                announce open-fires
            """);
        s.engine.trigger("quit", "Alex");
        assertTrue(s.runtime.chatter.contains("announce: quit-fires"));
        s.runtime.chatter.clear();

        s.engine.trigger(new Trigger("chat", "Alex").with("p", "hello"));
        assertTrue(s.runtime.chatter.contains("announce: chat-fires"));
        s.runtime.chatter.clear();

        s.engine.trigger(new Trigger("teleport", "Alex").with("p", "to spawn"));
        assertTrue(s.runtime.chatter.contains("announce: tp-fires"));
        s.runtime.chatter.clear();

        s.engine.trigger("swim", "Alex");
        assertTrue(s.runtime.chatter.contains("announce: swim-fires"));
        s.runtime.chatter.clear();

        s.engine.trigger("ignite", "Alex");
        assertTrue(s.runtime.chatter.contains("announce: burn-fires"));
        s.runtime.chatter.clear();

        s.engine.trigger(new Trigger("pickup", "Alex").with("p", "a diamond"));
        assertTrue(s.runtime.chatter.contains("announce: pickup-fires"));
        s.runtime.chatter.clear();

        s.engine.trigger(new Trigger("open", "Alex").with("p", "chest"));
        assertTrue(s.runtime.chatter.contains("announce: open-fires"));
    }

    // ------------------------------------------------------- add-on api

    @Test
    void pluginAddsActionsFunctionsConditionsAndEvents() {
        MockMcRuntime runtime = new MockMcRuntime();
        ScriptEngine engine = new ScriptEngine(runtime);
        engine.registerPlugin(new dev.verbum.api.VerbumPlugin() {
            @Override public String name() { return "rewards"; }
            @Override public void register(dev.verbum.api.EngineRegistrar reg) {
                reg.registerFunction(dev.verbum.api.NativeFunction.of("triple", (it, args, line) -> {
                    return Double.parseDouble(args.get(0)) * 3;
                }));
                reg.registerAction(dev.verbum.api.NativeAction.of("eureka", (it, words, line) -> {
                    it.runtime().tell(it.focus(), "eureka " + String.join(" ", words));
                }));
                reg.registerCondition(new dev.verbum.api.NativeCondition() {
                    @Override public boolean matches(List<String> words) {
                        return String.join(" ", words).toLowerCase().contains("mining titanium");
                    }
                    @Override public boolean eval(dev.verbum.interp.Interpreter it, List<String> words) { return true; }
                });
                reg.registerEvent(dev.verbum.api.EventWordMapper.fixed("escape", "escapes"));
            }
        });
        assertEquals(List.of("rewards"), engine.plugins());

        engine.load("""
            function double n
                return n * 2
            command test
                set player's t to call triple with 4
                set player's d to call double with 5
                eureka stone
                if player is mining titanium
                    announce titanium-detected
            when player escapes the void
                announce escaped
            """, "addon.vb");

        engine.interpreter().runCommand("test", List.of(), "Alex");
        var vars = engine.interpreter().store().playerVars("alex");
        assertEquals(12.0, (Double) vars.get("t"), 0.0001);
        assertEquals(10.0, (Double) vars.get("d"), 0.0001);
        assertTrue(runtime.chatter.contains("tell Alex: eureka stone"));
        assertTrue(runtime.chatter.contains("announce: titanium-detected"));

        engine.trigger("escape", "Alex");
        assertTrue(runtime.chatter.contains("announce: escaped"));
    }

    // ------------------------------------------------------- async

    @Test
    void afterBlockRunsLaterWithoutBlocking() {
        Scene s = new Scene("""
            command async1
                set player's a to 1
                after 0 seconds
                    set player's a to 2
                set player's b to player's a
            """);
        s.engine.interpreter().runCommand("async1", List.of(), "Alex");
        var vars = s.engine.interpreter().store().playerVars("alex");
        assertEquals(1.0, (Double) vars.get("a"), 0.0001);
        assertEquals(1.0, (Double) vars.get("b"), 0.0001);
        s.engine.tick();
        assertEquals(2.0, (Double) s.engine.interpreter().store().playerVars("alex").get("a"), 0.0001);
    }

    @Test
    void runLaterSchedulesRunnableOnTick() {
        Scene s = new Scene("""
            command async2
                set player's x to 0
            """);
        s.engine.interpreter().runCommand("async2", List.of(), "Alex");
        s.engine.interpreter().runLater(0, () ->
                s.engine.interpreter().store().set(dev.verbum.interp.VariableStore.Scope.PLAYER, "x", 9.0));
        assertEquals(0.0, (Double) s.engine.interpreter().store().playerVars("alex").get("x"), 0.0001);
        s.engine.tick();
        assertEquals(9.0, (Double) s.engine.interpreter().store().playerVars("alex").get("x"), 0.0001);
    }

    // ------------------------------------------------------- example script

    @Test
    void megaEventsExampleParsesAndHonoursCancel() throws Exception {
        try (var in = getClass().getClassLoader().getResourceAsStream("scripts/examples/mega_events.vb")) {
            assertNotNull(in, "mega_events.vb resource missing");
            String src = new String(in.readAllBytes());
            Scene s = new Scene(src);
            s.engine.trigger("join", "Alex");
            assertTrue(s.runtime.chatter.contains("announce: A VIP needs silence now"));
            assertFalse(s.runtime.chatter.stream().anyMatch(c -> c.equals("tell Alex: Welcome")));
        }
    }

    // ------------------------------------------------------- persistence

    @Test
    void variablesSurviveSaveAndReload() throws Exception {
        Path file = Files.createTempFile("verbum-test", ".json");
        try {
            MockMcRuntime runtime1 = new MockMcRuntime();
            ScriptEngine engine1 = new ScriptEngine(runtime1);
            engine1.load("""
                command seed
                    set {w::*} to village and nether and end
                    set player's coins to 42
                    set world score to 7
                """, "seed.vb");
            engine1.interpreter().runCommand("seed", List.of(), "Alex");
            engine1.saveVariables(file);

            MockMcRuntime runtime2 = new MockMcRuntime();
            ScriptEngine engine2 = new ScriptEngine(runtime2);
            engine2.loadVariables(file);
            engine2.load("""
                command check
                    tell player %{w::*}% and %player's coins% and %world score%
                """, "check.vb");
            engine2.interpreter().runCommand("check", List.of(), "Alex");
            assertTrue(runtime2.chatter.contains("tell Alex: [village, nether, end] and 42 and 7"));
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
package dev.verbum;

import dev.verbum.engine.ScriptEngine;
import dev.verbum.interp.VariableStore;
import dev.verbum.runtime.MockMcRuntime;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Skript-style {list::*} and {dict::key} variables: set/add/remove/contains/length/index. */
class InterpreterListTest {

    private static final class Scene {
        final MockMcRuntime runtime;
        final ScriptEngine engine;
        Scene(String src) {
            runtime = new MockMcRuntime();
            engine = new ScriptEngine(runtime);
            engine.load(src, "lists.vb");
        }
    }

    @Test
    void setAddContainsAndLength() {
        Scene s = new Scene("""
            command cave
                set player's warps::* to village and nether and end
                add the stronghold to player's warps::*
                if player's warps::* contains village
                    announce has-village
                if player's warps::* contains nether
                    announce has-nether
                if length of player's warps::* is at least 4
                    announce four
            """);
        s.engine.interpreter().runCommand("cave", List.of(), "Alex");
        Object raw = s.engine.interpreter().store().playerVars("alex").get("warps");
        assertTrue(raw instanceof List<?>);
        List<?> l = (List<?>) raw;
        assertEquals(4, l.size());
        assertTrue(l.contains("village") && l.contains("nether") && l.contains("end") && l.contains("the stronghold"));

        // also readable through the ::* spelling
        Object whole = s.engine.interpreter().store().get(VariableStore.Scope.PLAYER, "warps::*");
        assertTrue(whole instanceof List<?> && ((List<?>) whole).size() == 4);

        assertTrue(s.runtime.chatter.contains("announce: has-village"));
        assertTrue(s.runtime.chatter.contains("announce: has-nether"));
        assertTrue(s.runtime.chatter.contains("announce: four"));
    }

    @Test
    void indexReadWriteAndInterpolation() {
        Scene s = new Scene("""
            command tele
                set player's warps::* to village and nether and end
                set player's first to player's warps::1
                tell player Home is at %player's warps::1%
                set player's warps::2 to the stronghold
                set player's second to player's warps::2
            """);
        s.engine.interpreter().runCommand("tele", List.of(), "Alex");
        var vars = s.engine.interpreter().store().playerVars("alex");
        assertEquals("village", vars.get("first"));
        assertEquals("the stronghold", vars.get("second"));
        List<?> warps = (List<?>) vars.get("warps");
        assertEquals(List.of("village", "the stronghold", "end"), warps);
        assertTrue(s.runtime.chatter.contains("tell Alex: Home is at village"));
    }

    @Test
    void loopOverListGivesEachAndIndex() {
        Scene s = new Scene("""
            command tour
                set player's warps::* to village and nether
                for each w in player's warps::*
                    give player 1 w
                for each w in player's warps::*
                    tell player %loop-index%. %w%
            """);
        s.engine.interpreter().runCommand("tour", List.of(), "Alex");
        assertTrue(s.runtime.hasItem("Alex", "village", 1));
        assertTrue(s.runtime.hasItem("Alex", "nether", 1));
        assertTrue(s.runtime.chatter.contains("tell Alex: 1. village"));
        assertTrue(s.runtime.chatter.contains("tell Alex: 2. nether"));
    }

    @Test
    void isSetIsEmptyClearAndDelete() {
        Scene s = new Scene("""
            command tidy
                if player's warps::* is not set
                    announce was-unset
                delete player's warps::*
                if player's warps::* is empty
                    announce deleted-empty
                set player's warps::* to village
                if player's warps::* is set
                    announce is-set
                clear player's warps::*
                if player's warps::* is empty
                    announce cleared
            """);
        s.engine.interpreter().runCommand("tidy", List.of(), "Alex");
        assertTrue(s.runtime.chatter.contains("announce: was-unset"));
        assertTrue(s.runtime.chatter.contains("announce: deleted-empty"));
        assertTrue(s.runtime.chatter.contains("announce: is-set"));
        assertTrue(s.runtime.chatter.contains("announce: cleared"));
        Object v = s.engine.interpreter().store().get(VariableStore.Scope.PLAYER, "warps::*");
        assertTrue(v instanceof List<?> && ((List<?>) v).isEmpty());
    }

    @Test
    void dictSubscriptSetAndCompare() {
        Scene s = new Scene("""
            command shop
                set {price::sword} to 100
                if {price::sword} is set
                    announce price-set
                if {price::sword} is 100
                    announce price-100
            """);
        s.engine.interpreter().runCommand("shop", List.of(), "Alex");
        Object v = s.engine.interpreter().store().get(VariableStore.Scope.GLOBAL, "price::sword");
        assertEquals(100.0, (Double) v, 0.0001);
        assertTrue(s.runtime.chatter.contains("announce: price-set"));
        assertTrue(s.runtime.chatter.contains("announce: price-100"));
    }

    @Test
    void listsExampleScriptRuns() throws Exception {
        try (var in = getClass().getClassLoader().getResourceAsStream("scripts/examples/lists.vb")) {
            assertNotNull(in);
            String src = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            Scene s = new Scene(src);
            s.engine.interpreter().runCommand("addwarp", List.of("Village"), "Alex");
            s.engine.interpreter().runCommand("addwarp", List.of("Nether"), "Alex");
            s.engine.interpreter().runCommand("addwarp", List.of("Village"), "Alex");
            s.engine.interpreter().runCommand("warps", List.of(), "Alex");
            assertTrue(s.runtime.chatter.contains("tell Alex: 1. Village"));
            assertTrue(s.runtime.chatter.contains("tell Alex: 2. Nether"));
            s.engine.interpreter().runCommand("lastwarp", List.of(), "Alex");
            assertTrue(s.runtime.chatter.contains("tell Alex: Your first warp is Village"));
            s.engine.interpreter().runCommand("setupwarps", List.of(), "Alex");
            s.engine.interpreter().runCommand("goto", List.of("Village"), "Alex");
            assertTrue(s.runtime.log.stream().anyMatch(l -> l.contains("teleport Alex to village")));
            s.engine.interpreter().runCommand("catalog", List.of(), "Alex");
            assertTrue(s.runtime.chatter.contains("tell Alex: A bow costs 50 coins"));
        }
    }
}
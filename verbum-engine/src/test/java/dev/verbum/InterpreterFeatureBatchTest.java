package dev.verbum;

import dev.verbum.engine.ScriptEngine;
import dev.verbum.interp.VariableStore;
import dev.verbum.runtime.MockMcRuntime;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Skript-style list & text helpers, nested function calls, arg-N bindings,
 * loop-value, and live values (player's health, number of all players).
 */
class InterpreterFeatureBatchTest {

    private static final class Scene {
        final MockMcRuntime runtime;
        final ScriptEngine engine;
        Scene(String src) {
            runtime = new MockMcRuntime();
            engine = new ScriptEngine(runtime);
            engine.load(src, "features.vb");
        }
    }

    @Test
    void textCaseAndTrimHelpers() {
        Scene s = new Scene("""
            command fix
                set player's a to uppercase of hello
                set player's b to lowercase of WORLD
                set player's c to capitalize of jewel
                set player's spaced to hello there
                set player's d to trim of player's spaced
            """);
        s.engine.interpreter().runCommand("fix", List.of(), "Alex");
        var v = s.engine.interpreter().store().playerVars("alex");
        assertEquals("HELLO", v.get("a"));
        assertEquals("world", v.get("b"));
        assertEquals("Jewel", v.get("c"));
        assertEquals("hello there", v.get("d"));
    }

    @Test
    void joinOfBySeparator() {
        Scene s = new Scene("""
            command route
                set player's warps::* to village and nether and end
                set player's joined to join of player's warps::* by ,
            """);
        s.engine.interpreter().runCommand("route", List.of(), "Alex");
        Object r = s.engine.interpreter().store().playerVars("alex").get("joined");
        assertEquals("village,nether,end", r);
    }

    @Test
    void sortedReversedSizeAndIndices() {
        Scene s = new Scene("""
            command order
                set {w::*} to orange and apple and banana
                set {sorted::*} to alphabetically sorted {w::*}
                set {back::*} to reversed {w::*}
                set player's n to size of {w::*}
                set player's idx to indices of {w::*}
            """);
        s.engine.interpreter().runCommand("order", List.of(), "Alex");
        var store = s.engine.interpreter().store();
        assertEquals(List.of("apple", "banana", "orange"), store.get(VariableStore.Scope.GLOBAL, "sorted::*"));
        assertEquals(List.of("banana", "apple", "orange"), store.get(VariableStore.Scope.GLOBAL, "back::*"));
        var vars = store.playerVars("alex");
        assertEquals(3.0, (Double) vars.get("n"), 0.0001);
        assertEquals(List.of("1", "2", "3"), vars.get("idx"));
    }

    @Test
    void randomElementAndFirstLast() {
        Scene s = new Scene("""
            command pick
                set {w::*} to village and nether and end
                set player's pick to random element of {w::*}
                set player's first to first of the list {w::*}
                set player's last to last of the list {w::*}
            """);
        s.engine.interpreter().runCommand("pick", List.of(), "Alex");
        var vars = s.engine.interpreter().store().playerVars("alex");
        assertTrue(List.of("village", "nether", "end").contains(vars.get("pick")));
        assertEquals("village", vars.get("first"));
        assertEquals("end", vars.get("last"));
    }

    @Test
    void nestedFunctionCallsAndArithmetic() {
        Scene s = new Scene("""
            function double n
                return n * 2
            command calc
                set player's a to call double with 5
                set player's b to call double with call double with 5
                set player's c to call double with 5 plus 1
            """);
        s.engine.interpreter().runCommand("calc", List.of(), "Alex");
        var vars = s.engine.interpreter().store().playerVars("alex");
        assertEquals(10.0, (Double) vars.get("a"), 0.0001);
        assertEquals(20.0, (Double) vars.get("b"), 0.0001);
        assertEquals(12.0, (Double) vars.get("c"), 0.0001);
    }

    @Test
    void functionSeesArgNLoopValueAndLiveHealth() {
        Scene s = new Scene("""
            function double n
                return n * 2
            command report
                set {nums::*} to 2 and 4 and 6
                for each n in {nums::*}
                    set player's doubled to call double with loop-value
                    tell player Twice %loop-value% is %player's doubled%
                tell player Health: %player's health%
                set player's hp to player's health
                set player's alive to number of all players
            """);
        s.engine.interpreter().runCommand("report", List.of(), "Alex");
        var vars = s.engine.interpreter().store().playerVars("alex");
        assertEquals(12.0, (Double) vars.get("doubled"), 0.0001);   // last loop iteration: 6*2
        assertEquals(20.0, (Double) vars.get("hp"), 0.0001);
        assertEquals(1.0, (Double) vars.get("alive"), 0.0001);
        assertTrue(s.runtime.chatter.contains("tell Alex: Twice 6 is 12"));
        assertTrue(s.runtime.chatter.contains("tell Alex: Health: 20"));
    }

    @Test
    void argNVarsAndLastargAllargs() {
        Scene s = new Scene("""
            command greet
                tell player Hi %arg-1% and %arg-2%
                tell player Last was %lastarg%, all: %allargs%
            """);
        s.engine.interpreter().runCommand("greet", List.of("Alex", "Sam"), "Alex");
        assertTrue(s.runtime.chatter.contains("tell Alex: Hi Alex and Sam"));
        assertTrue(s.runtime.chatter.contains("tell Alex: Last was Sam, all: Alex Sam"));
    }

    @Test
    void numberOfLiveValuesInMathAndCondition() {
        Scene s = new Scene("""
            command poll
                if player's health is at least 20
                    announce full-hp
                set player's count to number of all players
                if number of all players is 1
                    announce solo
            """);
        s.engine.interpreter().runCommand("poll", List.of(), "Alex");
        assertEquals(1.0, (Double) s.engine.interpreter().store().playerVars("alex").get("count"), 0.0001);
        assertTrue(s.runtime.chatter.contains("announce: full-hp"));
        assertTrue(s.runtime.chatter.contains("announce: solo"));
    }
}
package dev.verbum;

import dev.verbum.engine.ScriptEngine;
import dev.verbum.interp.VariableStore;
import dev.verbum.runtime.MockMcRuntime;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Tests the Skript-beating feature batch: functions, cooldowns, braces, math helpers, item meta. */
class InterpreterAdvancedTest {

    private static final class Scene {
        final MockMcRuntime runtime;
        final ScriptEngine engine;
        Scene(String src) {
            runtime = new MockMcRuntime();
            engine = new ScriptEngine(runtime);
            engine.load(src, "adv.vb");
        }
    }

    @Test
    void functionReturnsValueIntoSet() {
        Scene s = new Scene("""
            function double number
                return number * 2
            command calc
                set player's result to call double with 5
            """);
        s.engine.interpreter().runCommand("calc", List.of(), "Alex");
        Object r = s.engine.interpreter().store().playerVars("alex").get("result");
        assertEquals(10.0, (Double) r, 0.0001);
    }

    @Test
    void functionWithTwoArgsAndFunctionInExpression() {
        Scene s = new Scene("""
            function maxOf a and b
                if a is greater than b
                    return a
                return b
            command calc
                set player's m to call maxOf with 3 and 9
                set player's t to call maxOf with 9 and 3
            """);
        s.engine.interpreter().runCommand("calc", List.of(), "Alex");
        var vars = s.engine.interpreter().store().playerVars("alex");
        assertEquals(9.0, (Double) vars.get("m"), 0.0001);
        assertEquals(9.0, (Double) vars.get("t"), 0.0001);
    }

    @Test
    void functionReturningTextAndEmptyReturn() {
        Scene s = new Scene("""
            function greet name
                return Hello name
            function nothing
                return
            command sayhi
                set player's greeting to call greet with Alex
                set player's empty to call nothing
            """);
        s.engine.interpreter().runCommand("sayhi", List.of(), "Alex");
        var vars = s.engine.interpreter().store().playerVars("alex");
        assertEquals("Hello Alex", vars.get("greeting"));
        assertEquals("", vars.get("empty"));
    }

    @Test
    void callAsStatementRunsFunctionForEffect() {
        Scene s = new Scene("""
            function reward player
                give player 1 diamond
                tell player Here is a diamond
            command gift
                call reward with player
            """);
        s.engine.interpreter().runCommand("gift", List.of(), "Alex");
        assertTrue(s.runtime.hasItem("Alex", "diamond", 1));
        assertTrue(s.runtime.chatter.contains("tell Alex: Here is a diamond"));
    }

    @Test
    void cooldownSkipsSecondRunSilently() {
        Scene s = new Scene("""
            command healme
                cooldown 5 seconds
                announce healed
            """);
        s.engine.interpreter().runCommand("healme", List.of(), "Alex");
        assertTrue(s.runtime.chatter.contains("announce: healed"));
        s.runtime.chatter.clear();
        s.engine.interpreter().runCommand("healme", List.of(), "Alex");
        assertTrue(s.runtime.chatter.isEmpty(), "second run should be blocked by cooldown");
    }

    @Test
    void cooldownIsPerPlayer() {
        Scene s = new Scene("""
            command boost
                cooldown 5 seconds
                announce boosted
            """);
        s.engine.interpreter().runCommand("boost", List.of(), "Alex");
        s.engine.interpreter().runCommand("boost", List.of(), "Steve");
        long boosted = s.runtime.chatter.stream().filter(c -> c.contains("boosted")).count();
        assertEquals(2, boosted, "each player gets their own cooldown");
    }

    @Test
    void braceVariablesWorkEverywhere() {
        Scene s = new Scene("""
            command stash
                set {coins} to 5
                set {player's gold} to {coins}
                set {world total} to {coins} plus 3
                if {coins} is 5
                    announce brace-works
            """);
        s.engine.interpreter().runCommand("stash", List.of(), "Alex");
        var vars = s.engine.interpreter().store().playerVars("alex");
        assertEquals(5.0, (Double) vars.get("gold"), 0.0001);
        Object total = s.engine.interpreter().store().get(VariableStore.Scope.WORLD, "total");
        assertEquals(8.0, (Double) total, 0.0001);
        assertTrue(s.runtime.chatter.contains("announce: brace-works"));
    }

    @Test
    void expressionMathFunctions() {
        Scene s = new Scene("""
            command mathy
                set player's a to floor of 4.7
                set player's b to ceil of 4.2
                set player's c to round of 4.5
                set player's d to absolute value of -3
                set player's e to sqrt of 16
                set player's f to max of 3 and 9
                set player's g to min of 3 and 9
            """);
        s.engine.interpreter().runCommand("mathy", List.of(), "Alex");
        var v = s.engine.interpreter().store().playerVars("alex");
        assertEquals(4.0, (Double) v.get("a"), 0.0001);
        assertEquals(5.0, (Double) v.get("b"), 0.0001);
        assertEquals(4.0, (Double) v.get("c"), 0.0001);
        assertEquals(3.0, (Double) v.get("d"), 0.0001);
        assertEquals(4.0, (Double) v.get("e"), 0.0001);
        assertEquals(9.0, (Double) v.get("f"), 0.0001);
        assertEquals(3.0, (Double) v.get("g"), 0.0001);
    }

    @Test
    void itemMetadataActionsLogToRuntime() {
        Scene s = new Scene("""
            command upgrade
                rename player's sword to Epic Sword
                lore player's sword to Shiny and Sharp
                modeldata player's sword to 100
            """);
        s.engine.interpreter().runCommand("upgrade", List.of(), "Alex");
        assertTrue(s.runtime.log.stream().anyMatch(l -> l.equals("rename Alex sword to Epic Sword")));
        assertTrue(s.runtime.log.stream().anyMatch(l -> l.equals("lore Alex sword to Shiny and Sharp")));
        assertTrue(s.runtime.log.stream().anyMatch(l -> l.equals("modeldata Alex sword to 100")));
    }

    @Test
    void thunderingCondition() {
        Scene s = new Scene("""
            command checkweather
                if it is thundering
                    announce stormy
            """);
        s.runtime.weather = "storm";
        s.engine.interpreter().runCommand("checkweather", List.of(), "Alex");
        assertTrue(s.runtime.chatter.contains("announce: stormy"));
    }

    @Test
    void percentInterpolationInMessages() {
        Scene s = new Scene("""
            command report
                set player's gold to 42
                set world leader to Alex
                announce %player's gold% stacked on top of %world leader%
            """);
        s.engine.interpreter().runCommand("report", List.of(), "Alex");
        assertTrue(s.runtime.chatter.stream().anyMatch(c -> c.contains("42 stacked on top of Alex")));
    }

    @Test
    void functionsExampleScriptRuns() {
        Scene s = new Scene("""
            function double number
                return number * 2
            function bigger a and b
                if a is greater than b
                    return a
                return b
            command shop
                set player's price to double 25
                set world last-price to bigger 45 and 40
                tell player Sword costs %world last-price%
            """);
        s.engine.interpreter().runCommand("shop", List.of(), "Alex");
        Object price = s.engine.interpreter().store().playerVars("alex").get("price");
        assertEquals(50.0, (Double) price, 0.0001);
        Object last = s.engine.interpreter().store().get(VariableStore.Scope.WORLD, "last-price");
        assertEquals(45.0, (Double) last, 0.0001);
        assertTrue(s.runtime.chatter.stream().anyMatch(c -> c.contains("Sword costs 45")));
    }
}

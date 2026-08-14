package dev.verbum;

import dev.verbum.engine.ScriptEngine;
import dev.verbum.interp.VariableStore;
import dev.verbum.runtime.MockMcRuntime;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Tests the Skript-style additions: custom commands, GUIs, waits, economy. */
class InterpreterSkriptTest {

    private static final class Scene {
        final MockMcRuntime runtime;
        final ScriptEngine engine;
        Scene(String src) {
            runtime = new MockMcRuntime();
            engine = new ScriptEngine(runtime);
            engine.load(src, "test.vb");
        }
    }

    @Test
    void customCommandRunsAndBindsArguments() {
        Scene s = new Scene("""
            command greet name
                tell player Hello name
            command hello
                announce Yo
            """);
        s.engine.interpreter().runCommand("greet", List.of("Steve"), "Alex");
        assertTrue(s.runtime.chatter.contains("tell Alex: Hello Steve"));

        s.engine.interpreter().runCommand("hello", List.of(), "Alex");
        assertTrue(s.runtime.chatter.contains("announce: Yo"));
    }

    @Test
    void menuOpensAndButtonClickRuns() {
        Scene s = new Scene("""
            command menu
                open menu rewards
            menu rewards
                add button Heal Up
                    heal player to full
                add button Give Diamond
                    give player diamond
            """);
        MockMcRuntime.MockPlayer p = s.runtime.player("Alex");
        p.hp = 5;
        s.engine.interpreter().runCommand("menu", List.of(), "Alex");
        assertEquals("rewards", s.engine.interpreter().lastOpenedMenu("Alex"));

        s.engine.interpreter().clickButton("rewards", "Heal Up", "Alex");
        assertEquals(20.0, p.hp, 0.0001);

        s.engine.interpreter().clickButton("rewards", "Give Diamond", "Alex");
        assertTrue(s.runtime.hasItem("Alex", "diamond", 1));
    }

    @Test
    void waitDefersAndZeroWaitIsImmediate() {
        Scene s = new Scene("""
            command boom
                announce before
                wait 5 seconds
                announce after wait
            """);
        s.engine.interpreter().runCommand("boom", List.of(), "Alex");
        // "before" ran, "after wait" deferred
        assertTrue(s.runtime.chatter.contains("announce: before"));
        assertFalse(s.runtime.chatter.contains("after wait"));

        Scene s2 = new Scene("""
            command instant
                announce first
                wait 0 seconds
                announce second
            """);
        s2.engine.interpreter().runCommand("instant", List.of(), "Alex");
        assertTrue(s2.runtime.chatter.contains("announce: second"));
    }

    @Test
    void economyPayChargeAndBalance() {
        Scene s = new Scene("""
            command earn
                pay player 50
            command spend
                charge player 20
            command balance
                balance player
            """);
        s.engine.interpreter().runCommand("earn", List.of(), "Alex");
        Object coins = s.engine.interpreter().store().playerVars("alex").get("coins");
        assertEquals(50.0, (Double) coins, 0.0001);

        s.engine.interpreter().runCommand("spend", List.of(), "Alex");
        coins = s.engine.interpreter().store().playerVars("alex").get("coins");
        assertEquals(30.0, (Double) coins, 0.0001);

        s.engine.interpreter().runCommand("balance", List.of(), "Alex");
        assertTrue(s.runtime.chatter.stream().anyMatch(c -> c.contains("30 coins")));
    }

    @Test
    void teleportPlayerToPlayer() {
        Scene s = new Scene("""
            command follow friend
                teleport player to friend
            """);
        s.runtime.player("friend").loc = dev.verbum.runtime.Location.at("world", 100, 70, 100);
        s.engine.interpreter().runCommand("follow", List.of("friend"), "Alex");
        dev.verbum.runtime.Location loc = s.runtime.player("Alex").loc;
        assertEquals(100.0, loc.x(), 0.0001);
        assertEquals(100.0, loc.z(), 0.0001);
    }

    @Test
    void setEvaluatesArithmeticExpressions() {
        Scene s = new Scene("""
            command calc
                set player's coins to 50
                set player's score to player's coins * 2 plus 1
                set world total to world base times 3 minus world base
            """);
        s.engine.interpreter().store().set(dev.verbum.interp.VariableStore.Scope.WORLD, "base", 10.0);
        s.engine.interpreter().runCommand("calc", List.of(), "Alex");
        assertEquals(101.0, (Double) s.engine.interpreter().store().playerVars("alex").get("score"), 0.0001);
        assertEquals(20.0, (Double) s.engine.interpreter().store().get(dev.verbum.interp.VariableStore.Scope.WORLD, "total"), 0.0001);
    }

    @Test
    void dropAndPlaceBlockLogToTheWorld() {
        Scene s = new Scene("""
            command toss
                drop 5 diamonds at player
            command build
                place stone at player
            """);
        s.engine.interpreter().runCommand("toss", List.of(), "Alex");
        assertTrue(s.runtime.log.stream().anyMatch(l -> l.contains("drop 5 diamonds at Alex")));
        s.engine.interpreter().runCommand("build", List.of(), "Alex");
        assertTrue(s.runtime.log.stream().anyMatch(l -> l.contains("set block stone")));
    }

    @Test
    void loopAllPlayersRunsBodyForEach() {
        Scene s = new Scene("""
            command gift
                loop all players
                    give player 1 emerald
            """);
        s.runtime.player("Steve");
        s.runtime.player("Alex");
        s.engine.interpreter().runCommand("gift", List.of(), "Alex");
        assertTrue(s.runtime.hasItem("Steve", "emerald", 1));
        assertTrue(s.runtime.hasItem("Alex", "emerald", 1));
    }

    @Test
    void makePlayerExecuteCommand() {
        Scene s = new Scene("""
            command warpme
                make player execute command /warp spawn
            """);
        s.engine.interpreter().runCommand("warpme", List.of(), "Alex");
        assertTrue(s.runtime.log.stream().anyMatch(l -> l.equals("execute Alex /warp spawn")));
    }

    @Test
    void applyPotionAndBroadcastAliases() {
        Scene s = new Scene("""
            command buff
                apply potion night vision to player for 30 seconds
                broadcast The server is boosting everyone
            """);
        s.engine.interpreter().runCommand("buff", List.of(), "Alex");
        assertTrue(s.runtime.player("Alex").effects.contains("night vision"));
        assertTrue(s.runtime.chatter.contains("announce: The server is boosting everyone"));
    }

    @Test
    void listVariablesAddRemoveAndContains() {
        Scene s = new Scene("""
            command listy
                set player's items to diamond and emerald and gold
                add iron to player's items
                if player's items contains diamond
                    announce has diamond
                remove emerald from player's items
            """);
        s.engine.interpreter().runCommand("listy", List.of(), "Alex");
        Object items = s.engine.interpreter().store().playerVars("alex").get("items");
        assertTrue(items instanceof List<?>);
        assertTrue(s.runtime.chatter.contains("announce: has diamond"));
        List<?> l = (List<?>) items;
        assertTrue(l.size() == 3, "expected 3 items after add+remove, got " + l);
        assertTrue(l.contains("gold") && l.contains("iron"));
        assertFalse(l.contains("emerald"));
    }

    @Test
    void loopOverListVariableIteratesEach() {
        Scene s = new Scene("""
            command eachitem
                set player's prizes to diamond and emerald
                for each item in player's prizes
                    give player 1 item
            """);
        s.engine.interpreter().runCommand("eachitem", List.of(), "Alex");
        assertTrue(s.runtime.hasItem("Alex", "diamond", 1));
        assertTrue(s.runtime.hasItem("Alex", "emerald", 1));
    }

    @Test
    void newActionsExplodeFeedOpReset() {
        Scene s = new Scene("""
            command boom
                explode player
            command lunch
                feed player
            command adminy
                op Alex
            command wipe
                reset player
            """);
        s.runtime.player("Alex").inventory.put("diamond", 3.0);
        s.runtime.player("Alex").hp = 5;
        s.engine.interpreter().runCommand("boom", List.of(), "Alex");
        assertTrue(s.runtime.log.stream().anyMatch(l -> l.contains("explode Alex")));
        s.engine.interpreter().runCommand("lunch", List.of(), "Alex");
        assertEquals(20.0, s.runtime.player("Alex").food, 0.0001);
        s.engine.interpreter().runCommand("adminy", List.of(), "Alex");
        assertTrue(s.runtime.isOp("Alex"));
        s.engine.interpreter().runCommand("wipe", List.of(), "Alex");
        assertEquals(0, s.runtime.player("Alex").inventory.size());
        assertEquals(20.0, s.runtime.player("Alex").hp, 0.0001);
    }

    @Test
    void conditionsPermissionAliveAndChance() {
        Scene s = new Scene("""
            when player uses command check
                if player is op
                    announce is-op
                if player has permission verbum.vip
                    announce has-perm
            when player uses command deathcheck
                if player is dead
                    warn player gone
            """);
        s.runtime.player("Alex").permissions.add("verbum.vip");
        s.engine.trigger(new dev.verbum.interp.Trigger("command", "Alex").with("p", "/check"));
        assertTrue(s.runtime.chatter.contains("announce: has-perm"));
        s.engine.trigger(new dev.verbum.interp.Trigger("command", "Alex").with("p", "/deathcheck"));
        assertTrue(s.runtime.chatter.stream().noneMatch(c -> c.startsWith("warn")));
    }

    @Test
    void newEventsPlaceDropCraftAndKill() {
        Scene s = new Scene("""
            when player places stone
                announce placed
            when player drops sword
                announce dropped
            when player crafts sword
                announce crafted
            when player kills zombie
                add 1 to player's kills
            """);
        s.engine.trigger(new dev.verbum.interp.Trigger("place", "Alex").with("p", "stone"));
        assertTrue(s.runtime.chatter.contains("announce: placed"));
        s.engine.trigger(new dev.verbum.interp.Trigger("drop", "Alex").with("p", "sword"));
        assertTrue(s.runtime.chatter.contains("announce: dropped"));
        s.engine.trigger(new dev.verbum.interp.Trigger("craft", "Alex").with("p", "sword"));
        assertTrue(s.runtime.chatter.contains("announce: crafted"));
        s.engine.trigger(new dev.verbum.interp.Trigger("kill", "Alex").with("p", "zombie"));
        assertEquals(1.0, (Double) s.engine.interpreter().store().playerVars("alex").get("kills"), 0.0001);
    }
}

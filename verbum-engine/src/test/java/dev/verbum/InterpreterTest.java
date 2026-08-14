package dev.verbum;

import dev.verbum.engine.ScriptEngine;
import dev.verbum.error.VerbumError;
import dev.verbum.interp.Trigger;
import dev.verbum.runtime.MockMcRuntime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InterpreterTest {

    private static final class Scene {
        final MockMcRuntime runtime;
        final ScriptEngine engine;
        Scene(String source) {
            runtime = new MockMcRuntime();
            engine = new ScriptEngine(runtime);
            engine.load(source, "test.vb");
        }
    }

    @Test
    void acceptanceFlowWorksEndToEnd() {
        Scene s = new Scene("""
            on server start
                define area victory area 0 0 200 200
            when player touches water
                kill player
            when player collects emerald
                add 1 to player's emeralds
            when player has 10 emeralds
                announce Player Wins
            when boss dies
                give all players 1 dragon egg
            """);
        s.engine.onServerStart();
        s.engine.trigger("join", "Steve");
        s.engine.trigger("touch water", "Steve");
        assertTrue(s.runtime.log.contains("kill Steve"));

        for (int i = 0; i < 10; i++) {
            s.engine.trigger(new Trigger("collect", "Steve").with("p", "emerald"));
            s.engine.tick();
        }
        assertTrue(s.runtime.chatter.contains("announce: Player Wins"));

        s.engine.trigger(new Trigger("boss death", "Boss"));
        assertTrue(s.runtime.log.stream().anyMatch(l -> l.contains("give Steve") && l.contains("dragon egg")));
    }

    @Test
    void variablesAddAndCompare() {
        Scene s = new Scene("""
            when player joins
                set player's coins to 100
                add 10 to player's coins
                remove 5 from player's coins
                if player's coins are at least 100
                    give player diamond
            """);
        s.engine.trigger("join", "Alex");
        assertTrue(s.runtime.hasItem("Alex", "diamond", 1));
    }

    @Test
    void customActionRunsBody() {
        Scene s = new Scene("""
            action reward player
                give player 10 diamonds
                tell player You received a reward
            when player completes quest
                reward player
            """);
        s.engine.trigger(new Trigger("complete", "Sam"));
        assertTrue(s.runtime.hasItem("Sam", "diamonds", 10));
        assertTrue(s.runtime.chatter.stream().anyMatch(c -> c.contains("You received a reward")));
    }

    @Test
    void loopsRepeatAndForEach() {
        Scene s = new Scene("""
            when player uses command mine
                repeat 5 times
                    spawn zombie
            when player joins
                for each online player
                    give player 1 emerald
            """);
        s.engine.trigger(new Trigger("command", "Bo").with("p", "/mine"));
        assertEquals(5, s.runtime.mobs.getOrDefault("zombie", java.util.List.of()).size());
    }

    @Test
    void ifElseIfElsePicksRightBranch() {
        Scene s = new Scene("""
            when player uses command test
                if player health is below 5
                    warn player low
                else if player health is below 15
                    warn player okay
                else
                    announce full
            """);
        MockMcRuntime.MockPlayer p = s.runtime.player("Casey");
        p.hp = 20;
        s.engine.trigger(new Trigger("command", "Casey").with("p", "/test"));
        assertTrue(s.runtime.chatter.stream().noneMatch(c -> c.startsWith("warn")));
        assertTrue(s.runtime.chatter.contains("announce: full"));
    }

    @Test
    void unknownActionGivesFriendlyError() {
        Scene s = new Scene("""
            when player joins
                frobnicate player
            """);
        VerbumError e = assertThrows(VerbumError.class, () -> s.engine.trigger("join", "Lee"));
        assertTrue(e.getMessage().toLowerCase().contains("do not know the action"));
        assertTrue(e.getMessage().contains("give"));
    }

    @Test
    void repeatWhileAndUntil() {
        Scene s = new Scene("""
            when player uses command grind
                set world gold to 0
                repeat while world gold is less than 5
                    add 1 to world gold
                until world gold is at least 8
                    add 1 to world gold
            """);
        s.engine.trigger(new Trigger("command", "Mo").with("p", "/grind"));
        Object gold = s.engine.interpreter().store().get(dev.verbum.interp.VariableStore.Scope.WORLD, "gold");
        assertEquals(8.0, (Double) gold, 0.0001);
    }

    @Test
    void persistenceSaveAndLoad() {
        Scene s = new Scene("""
            when player uses command save
                set player's coins to 50
                save player's coins to database
            when player uses command load
                load player's coins
                give player diamonds
            """);
        s.engine.trigger(new Trigger("command", "Mo").with("p", "/save"));
        assertEquals(50.0, (Double) s.runtime.loadPersistent(
                "coins:PLAYER:Mo"), 0.0001);
    }
}

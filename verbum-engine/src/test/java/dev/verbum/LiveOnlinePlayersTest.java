package dev.verbum;

import dev.verbum.engine.ScriptEngine;
import dev.verbum.interp.VariableStore;
import dev.verbum.runtime.MockMcRuntime;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for bug fixes:
 *  - condition-style  when  handlers must iterate the *runtime's* online players
 *    (on a live Paper server that list is real, so  when player has X  fires).
 *  - a fresh engine on the *same* runtime still sees online players (reloads).
 */
class LiveOnlinePlayersTest {

    /** A runtime that behaves like the mock world but reports two fixed online players. */
    private static final class FakePaperRuntime extends MockMcRuntime {
        @Override public List<String> onlinePlayerNames() {
            return List.of("Ann", "Bob");
        }
    }

    private static void giveCoins(ScriptEngine engine, String player, double coins) {
        engine.interpreter().store().setFocus(player);
        engine.interpreter().store().set(VariableStore.Scope.PLAYER, "coins", coins);
    }

    @Test
    void conditionEventsFireForEveryLivePlayer() {
        FakePaperRuntime runtime = new FakePaperRuntime();
        ScriptEngine engine = new ScriptEngine(runtime);
        engine.load("""
            when player's coins is at least 3
                tell player Nice
            """, "live-test.vb");

        giveCoins(engine, "Ann", 5);
        giveCoins(engine, "Bob", 7);
        engine.tick();

        assertTrue(runtime.chatter.contains("tell Ann: Nice"),
                "Ann should get the condition event but log was: " + runtime.chatter);
        assertTrue(runtime.chatter.contains("tell Bob: Nice"),
                "Bob should get the condition event but log was: " + runtime.chatter);
    }

    @Test
    void freshEngineOnSameRuntimeStillSeesOnlinePlayers() {
        FakePaperRuntime runtime = new FakePaperRuntime();
        // Simulate /verbum reload: a brand-new engine but the same runtime.
        ScriptEngine first = new ScriptEngine(runtime);
        first.load("""
            when player's coins is at least 3
                tell player First
            """, "a.vb");

        ScriptEngine reloaded = new ScriptEngine(runtime);
        reloaded.load("""
            when player's coins is at least 3
                tell player Reloaded
            """, "b.vb");

        giveCoins(reloaded, "Ann", 4);
        reloaded.tick();

        assertTrue(runtime.chatter.contains("tell Ann: Reloaded"),
                "The reloaded engine must read live players from the shared runtime");
    }
}
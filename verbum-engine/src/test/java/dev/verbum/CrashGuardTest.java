package dev.verbum;

import dev.verbum.engine.ScriptEngine;
import dev.verbum.error.VerbumError;
import dev.verbum.runtime.MockMcRuntime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for past crash bugs (IndexOutOfBounds, OutOfMemory, hangs
 * and IllegalArgumentException) that should surface as friendly VerbumErrors.
 */
class CrashGuardTest {

    private static final class Scene {
        final MockMcRuntime runtime;
        final ScriptEngine engine;
        Scene(String source) {
            runtime = new MockMcRuntime();
            engine = new ScriptEngine(runtime);
            engine.load(source, "crash-test.vb");
        }
    }

    @Test
    void emptyGiveTellsUserWhatItNeeds() {
        Scene s = new Scene("""
            when player joins
                give
            """);
        VerbumError e = assertThrows(VerbumError.class, () -> s.engine.trigger("join", "Steve"));
        assertTrue(e.getMessage().contains("give"), () -> e.getMessage());
    }

    @Test
    void emptyTakeTellsUserWhatItNeeds() {
        Scene s = new Scene("""
            when player joins
                take
            """);
        VerbumError e = assertThrows(VerbumError.class, () -> s.engine.trigger("join", "Steve"));
        assertTrue(e.getMessage().contains("take"), () -> e.getMessage());
    }

    @Test
    void sidebarLineZeroDoesNotCrash() {
        Scene s = new Scene("""
            when player joins
                set sidebar line 0 to hello
                set sidebar line 3 to world
            """);
        s.engine.trigger("join", "Steve");
        assertTrue(s.runtime.log.contains("sidebar line 1 hello"));
        assertTrue(s.runtime.log.contains("sidebar line 3 world"));
    }

    @Test
    void hugeListIndexGivesFriendlyErrorInsteadOfOom() {
        Scene s = new Scene("""
            when player joins
                set player's list::999999999999999999 to x
            """);
        VerbumError e = assertThrows(VerbumError.class, () -> s.engine.trigger("join", "Steve"));
        assertTrue(e.getMessage().toLowerCase().contains("too big"), () -> e.getMessage());
    }

    @Test
    void trailingOperatorGivesFriendlyError() {
        Scene s = new Scene("""
            when player joins
                set x to 5 plus
            """);
        VerbumError e = assertThrows(VerbumError.class, () -> s.engine.trigger("join", "Steve"));
        assertTrue(e.getMessage().contains("number"), () -> e.getMessage());
    }

    @Test
    void playersOnlineComparisonEvaluatesInsteadOfBaguing() {
        Scene s = new Scene("""
            when player joins
                if server has more than 2 players online
                    announce many
            """);
        for (String n : new String[]{"Bob", "Carol", "Dave", "Eve"}) s.runtime.player(n);
        s.engine.trigger("join", "Steve");
        assertTrue(s.runtime.chatter.contains("announce: many"));
    }

    @Test
    void reversedRandomBoundsDoNotThrow() {
        Scene s = new Scene("""
            when player joins
                set x to random between 5 and 1 plus 2
                give player {x}
            """);
        s.engine.trigger("join", "Steve");
        assertTrue(s.runtime.log.stream().anyMatch(l -> l.contains("give")));
    }

    @Test
    void hugeRepeatCountGivesFriendlyErrorInsteadOfHanging() {
        Scene s = new Scene("""
            when player joins
                repeat 999999999 times
                    give player apple
            """);
        VerbumError e = assertThrows(VerbumError.class, () -> s.engine.trigger("join", "Steve"));
        assertTrue(e.getMessage().contains("repeat"), () -> e.getMessage());
    }

    @Test
    void zeroListIndexGivesFriendlyError() {
        Scene s = new Scene("""
            when player joins
                set player's list::0 to x
            """);
        VerbumError e = assertThrows(VerbumError.class, () -> s.engine.trigger("join", "Steve"));
        assertTrue(e.getMessage().contains("position"), () -> e.getMessage());
    }
}
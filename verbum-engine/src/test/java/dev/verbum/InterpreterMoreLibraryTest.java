package dev.verbum;

import dev.verbum.engine.ScriptEngine;
import dev.verbum.interp.Trigger;
import dev.verbum.runtime.McRuntime;
import dev.verbum.runtime.MockMcRuntime;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Round two of the Big Library: chat & messages, player meta & state,
 * world & environment, entities & mobs and systems & logic, all verified
 * on the Mock runtime.
 */
class InterpreterMoreLibraryTest {

    private static final class Scene {
        final MockMcRuntime runtime;
        final ScriptEngine engine;
        Scene(String src) {
            runtime = new MockMcRuntime();
            engine = new ScriptEngine(runtime);
            engine.load(src, "library2.vb");
        }
    }

    // ------------------------------------------------------- chat & messages

    @Test
    void chatMessagesAndChatControl() {
        Scene s = new Scene("""
            command chat
                set join message to Welcome everyone
                set quit message to Goodbye everyone
                set public chat off
                set private chat off
                clear chat Alex
                send hover message Alex Hello with tooltip Click me
                send clickable text Alex Click with command /tp
                hide chat
            """);
        s.engine.interpreter().runCommand("chat", List.of(), "Alex");
        assertEquals("Welcome everyone", s.runtime.joinMessage);
        assertEquals("Goodbye everyone", s.runtime.quitMessage);
        assertFalse(s.runtime.publicChat);
        assertFalse(s.runtime.privateChat);
        assertTrue(s.runtime.clearedChat.contains("Alex"));
        assertTrue(s.runtime.hoverMessages.contains("Hello | Click me"));
        assertTrue(s.runtime.clickMessages.contains("Click | /tp"));
    }

    // ------------------------------------------------------- player meta & state

    @Test
    void playerStateActions() {
        Scene s = new Scene("""
            command meta
                set sneaking Alex
                set sprinting Alex
                vanish Alex
                hide Alex from Bob
                set invincible Alex
                set fall protection Alex
                set armor points Alex to 8
                set absorption Alex to 4
                set cooldown Alex attack for 5 seconds
            """);
        s.engine.interpreter().runCommand("meta", List.of(), "Alex");
        assertTrue(s.runtime.sneakingState.getOrDefault("alex", false));
        assertTrue(s.runtime.sprintingState.getOrDefault("alex", false));
        assertTrue(s.runtime.hiddenFrom.getOrDefault("alex", List.of()).contains(McRuntime.ALL));
        assertTrue(s.runtime.hiddenFrom.getOrDefault("alex", List.of()).contains("Bob"));
        assertTrue(s.runtime.invinciblePlayers.getOrDefault("alex", false));
        assertTrue(s.runtime.noFallDamage.getOrDefault("alex", false));
        assertTrue(s.runtime.hasCooldown("Alex", "attack"));
        assertFalse(s.runtime.hasCooldown("Alex", "magic"));
    }

    @Test
    void revealUndoesVanish() {
        Scene s = new Scene("""
            command rev
                vanish Alex
                unvanish Alex
                show Alex to Bob
            """);
        s.engine.interpreter().runCommand("rev", List.of(), "Alex");
        var hidden = s.runtime.hiddenFrom.getOrDefault("alex", List.of());
        assertFalse(hidden.contains(McRuntime.ALL), "should no longer be hidden from everyone");
        assertFalse(hidden.contains("Bob"), "should no longer be hidden from Bob");
    }

    // ------------------------------------------------------- world & environment

    @Test
    void worldAndEnvironmentActions() {
        Scene s = new Scene("""
            command world
                set weather duration 300
                make storm
                make thunder
                set time speed 3
                set player limit 20
                spawn structure castle at Spawn
            """);
        s.engine.interpreter().runCommand("world", List.of(), "Alex");
        assertEquals(300.0, s.runtime.weatherDuration, 0.0001);
        assertTrue(s.runtime.storm);
        assertTrue(s.runtime.thunder);
        assertEquals(3.0, s.runtime.timeSpeed, 0.0001);
        assertEquals(20, s.runtime.playerLimit);
        assertTrue(s.runtime.structures.contains("castle @ Spawn"));
    }

    // ------------------------------------------------------- entities & mobs

    @Test
    void mobControlActions() {
        Scene s = new Scene("""
            command mobs
                set mob ai zombie off
                set mob breeding zombie off
                set mob gravity boss off
                set mob flying phantom on
                set mob drop zombie to diamond
                set mob follow zombie to Alex
                set mob target zombie to Alex
                set mob name visible hero off
                set mob persistent zombie
            """);
        s.engine.interpreter().runCommand("mobs", List.of(), "Alex");
        assertFalse(s.runtime.mobAi.getOrDefault("zombie", true));
        assertFalse(s.runtime.mobBreeding.getOrDefault("zombie", true));
        assertFalse(s.runtime.mobGravity.getOrDefault("boss", true));
        assertTrue(s.runtime.mobFlying.getOrDefault("phantom", false));
        assertEquals("diamond", s.runtime.mobCustomDrops.get("zombie"));
        assertEquals("Alex", s.runtime.mobFollows.get("zombie"));
        assertEquals("Alex", s.runtime.mobTargets.get("zombie"));
        assertFalse(s.runtime.mobNameVisible.getOrDefault("hero", true));
        assertTrue(s.runtime.mobPersistent.getOrDefault("zombie", false));
    }

    // ------------------------------------------------------- systems & logic

    @Test
    void sidebarActions() {
        Scene s = new Scene("""
            command score
                set sidebar title Welcome
                set sidebar line 1 to Hello
                set sidebar line 2 to Points: 5
            """);
        s.engine.interpreter().runCommand("score", List.of(), "Alex");
        assertEquals("Welcome", s.runtime.sidebarTitle);
        assertEquals("Hello", s.runtime.sidebarLines.get(0));
        assertEquals("Points: 5", s.runtime.sidebarLines.get(1));
    }

    // ------------------------------------------------------- more events

    @Test
    void newEventSentencesTrigger() {
        Scene s = new Scene("""
            when player enters a portal
                announce portal-hit
            when player gets damaged by a creeper
                announce hurt-hit
            when player consumes a potion
                announce drank
            """);
        s.engine.trigger(new Trigger("portal", "Alex"));
        s.engine.trigger(new Trigger("hurt", "Alex"));
        s.engine.trigger(new Trigger("consume", "Alex"));
        assertTrue(s.runtime.chatter.contains("announce: portal-hit"));
        assertTrue(s.runtime.chatter.contains("announce: hurt-hit"));
        assertTrue(s.runtime.chatter.contains("announce: drank"));
    }
}

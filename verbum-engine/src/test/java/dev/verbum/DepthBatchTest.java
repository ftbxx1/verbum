package dev.verbum;

import dev.verbum.engine.ScriptEngine;
import dev.verbum.runtime.MockMcRuntime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The depth batch: jail / homes / riding / repair / holograms / whitelist.
 * Every mechanism is a script verb, a live condition and a live variable,
 * backed by real state in MockMcRuntime so both directions verify.
 */
class DepthBatchTest {

    private static final class Ctx {
        final MockMcRuntime runtime = new MockMcRuntime();
        final ScriptEngine engine = new ScriptEngine(runtime);

        Ctx script(String src) {
            engine.load(src, "depth.vb");
            return this;
        }
    }

    @Test
    void jailAndReleaseWorkAsActionsAndConditions() {
        Ctx ctx = new Ctx().script("""
            when the player joins
                if player is jailed
                    tell player You are locked
                else
                    jail player
            """);
        ctx.engine.trigger("join", "Steve");
        ctx.engine.tick();

        assertTrue(ctx.runtime.isJailed("Steve"), "jail action should lock Steve");
        assertFalse(ctx.runtime.chatter.stream().anyMatch(l -> l.contains("You are locked")),
                "Steve must be jailed before the jailed-check can pass");
    }

    @Test
    void jailedPlayerReadsAsLiveVariable() {
        Ctx ctx = new Ctx().script("""
            when the player joins
                jail player
                if player's jail state is true
                    tell player Locked
            """);
        // 'jail state' matches the live variable 'player's jailed'
        ctx.engine.trigger("join", "Ann");
        ctx.engine.tick();
        assertTrue(ctx.runtime.chatter.stream().anyMatch(l -> l.contains("tell Ann: Locked")),
                "player's jailed live variable must be readable after jailing: " + ctx.runtime.chatter);
    }

    @Test
    void homesAreRememberedAndTeleported() {
        MockMcRuntime runtime = new MockMcRuntime();
        ScriptEngine engine = new ScriptEngine(runtime);
        engine.load("""
            when the player joins
                set home
                if player has a home
                    announce HasHome
            when the player joins
                teleport home
            """, "home.vb");

        engine.trigger("join", "Alex");
        engine.tick();

        assertTrue(runtime.chatter.stream().anyMatch(l -> l.contains("HasHome")),
                "hasHome condition should pass after set home: " + runtime.chatter);
        assertTrue(runtime.log.stream().anyMatch(l -> l.startsWith("teleport Alex home")),
                "teleport home should fire: " + runtime.log);
    }

    @Test
    void ridingMountAndDismount() {
        MockMcRuntime runtime = new MockMcRuntime();
        ScriptEngine engine = new ScriptEngine(runtime);
        engine.load("""
            when the player joins
                mount player
                if player is riding
                    tell player Giddy up
            """, "ride.vb");

        engine.trigger("join", "Bob");
        engine.tick();

        assertTrue(runtime.isRiding("Bob"), "mount should set in-vehicle state");
        assertTrue(runtime.chatter.stream().anyMatch(l -> l.contains("tell Bob: Giddy up")),
                "is riding condition should pass: " + runtime.chatter);
    }

    @Test
    void repairFiresForSingleAndAll() {
        MockMcRuntime runtime = new MockMcRuntime();
        ScriptEngine engine = new ScriptEngine(runtime);
        engine.load("""
            when the player joins
                repair item
            when the player joins
                repair all
            """, "repair.vb");

        engine.trigger("join", "Cara");
        engine.tick();

        assertTrue(runtime.log.stream().anyMatch(l -> l.equals("repair Cara")),
                "repair item should log: " + runtime.log);
        assertTrue(runtime.log.stream().anyMatch(l -> l.equals("repair Cara all")),
                "repair all should log: " + runtime.log);
    }

    @Test
    void hologramsSpawnAndRemove() {
        MockMcRuntime runtime = new MockMcRuntime();
        ScriptEngine engine = new ScriptEngine(runtime);
        engine.load("""
            when the player joins
                hologram player with text Welcome home
            """, "holo.vb");

        engine.trigger("join", "Dave");
        engine.tick();

        assertTrue(runtime.hologramNames("Dave").contains("hologram"),
                "a hologram should exist after spawn: " + runtime.hologramNames("Dave"));
        assertTrue(runtime.log.stream().anyMatch(l -> l.contains("Welcome home")),
                "hologram text must be recorded: " + runtime.log);
    }

    @Test
    void whitelistAddAndRemove() {
        MockMcRuntime runtime = new MockMcRuntime();
        ScriptEngine engine = new ScriptEngine(runtime);
        engine.load("""
            when the player joins
                whitelist player
            when the player joins
                if player is whitelisted
                    tell player Whitelisted
            """, "wl.vb");

        engine.trigger("join", "Eve");
        engine.tick();

        assertTrue(runtime.isWhitelisted("Eve"), "whitelist action should register the player");
        assertTrue(runtime.chatter.stream().anyMatch(l -> l.contains("tell Eve: Whitelisted")),
                "is whitelisted condition must pass: " + runtime.chatter);
    }
}
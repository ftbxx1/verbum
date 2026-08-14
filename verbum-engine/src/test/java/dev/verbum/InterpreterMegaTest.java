package dev.verbum;

import dev.verbum.engine.ScriptEngine;
import dev.verbum.interp.Trigger;
import dev.verbum.runtime.MockMcRuntime;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Round three of the Big Library: live-stat variables, environment & gear
 * conditions, a flood of new events, and new actions (enchanted items,
 * colored fireworks, kill/death tracking).
 */
class InterpreterMegaTest {

    private static final class Scene {
        final MockMcRuntime runtime;
        final ScriptEngine engine;
        Scene(String src) {
            runtime = new MockMcRuntime();
            engine = new ScriptEngine(runtime);
            engine.load(src, "mega.vb");
        }
    }

    // ------------------------------------------------------- live stats variables

    @Test
    void liveStatConditions() {
        Scene s = new Scene("""
            command stats
                if player has 5 kills
                    announce five-kills
                if player kill streak is at least 3
                    announce streak-on
                if player deaths are less than 2
                    announce few-deaths
                if player armor is at least 8
                    announce armored
                if player health percent is more than 50
                    announce healthy
            """);
        s.runtime.killCounts.put("alex", 5);
        s.runtime.killStreaks.put("alex", 3);
        s.runtime.deathCounts.put("alex", 1);
        s.runtime.player("Alex").armorValue = 10;
        s.runtime.player("Alex").hp = 15;
        s.engine.interpreter().runCommand("stats", List.of(), "Alex");
        assertTrue(s.runtime.chatter.contains("announce: five-kills"));
        assertTrue(s.runtime.chatter.contains("announce: streak-on"));
        assertTrue(s.runtime.chatter.contains("announce: few-deaths"));
        assertTrue(s.runtime.chatter.contains("announce: armored"));
        assertTrue(s.runtime.chatter.contains("announce: healthy"));
    }

    @Test
    void statActionsTrackCounts() {
        Scene s = new Scene("""
            command track
                add a kill Alex
                add a kill Alex
                add a death Alex
            """);
        s.engine.interpreter().runCommand("track", List.of(), "Alex");
        assertEquals(2, s.runtime.killCounts.getOrDefault("alex", 0));
        assertEquals(1, s.runtime.deathCounts.getOrDefault("alex", 0));
        assertEquals(0, s.runtime.killStreaks.getOrDefault("alex", 0), "death should reset the streak");
    }

    // ------------------------------------------------------- environment & gear

    @Test
    void environmentAndGearConditions() {
        Scene s = new Scene("""
            command env
                if player is in lava
                    announce lava
                if player is in bed
                    announce bed
                if player is under the open sky
                    announce sky
                if player weapon is diamond sword
                    announce sword
                if distance from Alex to Bob is at least 5
                    announce far
            """);
        MockMcRuntime.MockPlayer alex = s.runtime.player("Alex");
        alex.inLava = true;
        alex.inBed = true;
        alex.underSky = true;
        alex.weapon = "diamond sword";
        s.runtime.player("Bob").loc = dev.verbum.runtime.Location.at("world", 0, 64, 10);
        s.engine.interpreter().runCommand("env", List.of(), "Alex");
        assertTrue(s.runtime.chatter.contains("announce: lava"));
        assertTrue(s.runtime.chatter.contains("announce: bed"));
        assertTrue(s.runtime.chatter.contains("announce: sky"));
        assertTrue(s.runtime.chatter.contains("announce: sword"));
        assertTrue(s.runtime.chatter.contains("announce: far"));
    }

    // ------------------------------------------------------- more events

    @Test
    void newEventSentencesTrigger() {
        Scene s = new Scene("""
            when player first joins
                announce first
            when it starts raining
                announce rain
            when player takes fall damage
                announce ouch
            when player jumps
                announce hop
            when player opens a chest
                announce chest
            when player right clicks on a villager
                announce villager
            when player goes to bed
                announce sleepy
            when day starts
                announce dawn
            """);
        s.engine.trigger(new Trigger("open", "Alex").with("p", "chest"));
        for (String kind : new String[]{"firstjoin", "rainstart", "fall", "jump",
                "rightclick", "sleep", "day"}) {
            s.engine.trigger(new Trigger(kind, "Alex"));
        }
        assertTrue(s.runtime.chatter.contains("announce: first"));
        assertTrue(s.runtime.chatter.contains("announce: rain"));
        assertTrue(s.runtime.chatter.contains("announce: ouch"));
        assertTrue(s.runtime.chatter.contains("announce: hop"));
        assertTrue(s.runtime.chatter.contains("announce: chest"));
        assertTrue(s.runtime.chatter.contains("announce: villager"));
        assertTrue(s.runtime.chatter.contains("announce: sleepy"));
        assertTrue(s.runtime.chatter.contains("announce: dawn"));
    }

    // ------------------------------------------------------- enchanted items & fireworks

    @Test
    void enchantedGiveAndColoredFireworks() {
        Scene s = new Scene("""
            command gear
                give player a diamond sword with sharpness 5
                shoot a red firework
                shoot a blue firework
            """);
        s.engine.interpreter().runCommand("gear", List.of(), "Alex");
        assertTrue(s.runtime.log.stream().anyMatch(l -> l.equals("enchanted Alex diamond sword sharpness 5")),
                "log was: " + s.runtime.log);
        assertTrue(s.runtime.log.stream().anyMatch(l -> l.equals("firework Alex red")));
        assertTrue(s.runtime.log.stream().anyMatch(l -> l.equals("firework Alex blue")));
    }
}
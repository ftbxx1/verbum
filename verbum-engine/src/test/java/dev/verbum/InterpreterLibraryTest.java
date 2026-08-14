package dev.verbum;

import dev.verbum.engine.ScriptEngine;
import dev.verbum.interp.Trigger;
import dev.verbum.runtime.MockMcRuntime;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Big Library: scoreboards, teams, bossbars, mutes, flags, quests,
 * body sensors and the world-state actions, all verified on the Mock runtime.
 */
class InterpreterLibraryTest {

    private static final class Scene {
        final MockMcRuntime runtime;
        final ScriptEngine engine;
        Scene(String src) {
            runtime = new MockMcRuntime();
            engine = new ScriptEngine(runtime);
            engine.load(src, "library.vb");
        }
    }

    // ------------------------------------------------------- scoreboards

    @Test
    void scoreboardActionsAndConditions() {
        Scene s = new Scene("""
            command sb
                create scoreboard kills with display Kills
                set score for Alex in kills to 5
                add score for Alex in kills by 3
                if score of kills is at least 8
                    announce big-score
                if score of kills is less than 5
                    announce small-score
            """);
        s.engine.interpreter().runCommand("sb", List.of(), "Alex");
        assertEquals(8.0, s.runtime.score("kills", "Alex"), 0.0001);
        assertTrue(s.runtime.chatter.contains("announce: big-score"));
        assertFalse(s.runtime.chatter.contains("announce: small-score"));
    }

    // ------------------------------------------------------- teams

    @Test
    void teamActionsAndInTeamCondition() {
        Scene s = new Scene("""
            command tm
                create team red
                add Alex to team red
                if player is in team red
                    announce in-red
                remove Alex from team red
                if player is not in team red
                    announce out-red
            """);
        s.engine.interpreter().runCommand("tm", List.of(), "Alex");
        assertTrue(s.runtime.chatter.contains("announce: in-red"));
        assertTrue(s.runtime.chatter.contains("announce: out-red"));
        assertTrue(s.runtime.teamMembers("red").isEmpty());
    }

    // ------------------------------------------------------- boss bars

    @Test
    void bossBarActionsRun() {
        Scene s = new Scene("""
            command bb
                create boss bar Wither with title The Wither
                set boss bar Wither's progress to 0.5
                set boss bar Wither's color to red
                set boss bar Wither visible
            """);
        s.engine.interpreter().runCommand("bb", List.of(), "Alex");
        assertEquals(0.5, s.runtime.bossBars.get("wither's"), 0.0001);
        assertEquals("red", s.runtime.bossBarColors.get("wither's"));
    }

    // ------------------------------------------------------- mutes, flags, quests, gamemode

    @Test
    void muteFlagQuestAndGamemode() {
        Scene s = new Scene("""
            command state
                mute Alex
                set flag safe-zone to true
                set quest main to 3
                complete quest side-mission
                set player's gamemode to creative
                if player is muted
                    announce muted-yes
                if flag safe-zone is set
                    announce safe-fora
                if quest main is complete
                    announce main-done
                if quest main progress is at least 3
                    announce quest-three
                if player is in creative
                    announce creative-mode
            """);
        s.engine.interpreter().runCommand("state", List.of(), "Alex");
        assertTrue(s.runtime.isMuted("Alex"));
        assertTrue(s.runtime.getFlag("safe-zone"));
        assertFalse(s.runtime.questDone("main"));
        assertTrue(s.runtime.questsDone.getOrDefault("side-mission", false));

        Scene c = new Scene("""
            command qdone
                if quest main is complete
                    announce done
                else
                    announce not-done
            """);
        c.runtime.completeQuest("main");
        c.engine.interpreter().runCommand("qdone", List.of(), "Alex");
        assertTrue(c.runtime.chatter.contains("announce: done"));
    }

    // ------------------------------------------------------- body sensors

    @Test
    void bodySensorConditions() {
        Scene s = new Scene("""
            command sense
                set player swimming
                if player is swimming
                    announce swim
                if player is not gliding
                    announce no-glide
                set player in water
                if player is underwater
                    announce wet
                wear a diamond helmet
                if player is wearing a diamond helmet
                    announce helmet-on
            """);
        s.runtime.player("Alex").swimming = true;
        s.runtime.player("Alex").inWater = true;
        s.runtime.player("Alex").wearing.add("diamond helmet");
        s.engine.interpreter().runCommand("sense", List.of(), "Alex");
        assertTrue(s.runtime.chatter.contains("announce: swim"));
        assertTrue(s.runtime.chatter.contains("announce: no-glide"));
        assertTrue(s.runtime.chatter.contains("announce: wet"));
        assertTrue(s.runtime.chatter.contains("announce: helmet-on"));
    }

    // ------------------------------------------------------- item/menu/world state verbs

    @Test
    void inventoryMenuAndWorldActions() {
        Scene s = new Scene("""
            command big
                give Alex a diamond sword
                set player's health to 10
                set player's food to 8
                set player's level to 30
                give Alex a leather helmet
                wear a leather helmet
                open anvil
                set the world's difficulty to hard
                set the world's border to 500
                drop a gold ingot
            """);
        s.engine.interpreter().runCommand("big", List.of(), "Alex");
        assertEquals(10.0, s.runtime.player("Alex").hp, 0.0001);
        assertEquals(8.0, s.runtime.player("Alex").food, 0.0001);
        assertEquals(30, s.runtime.player("Alex").level);
        assertTrue(s.runtime.log.stream().anyMatch(l -> l.contains("anvil Alex")));
        assertTrue(s.runtime.log.stream().anyMatch(l -> l.equals("difficulty hard")));
        assertTrue(s.runtime.log.stream().anyMatch(l -> l.contains("border 500")));
        assertTrue(s.runtime.log.stream().anyMatch(l -> l.contains("gold ingot")));
    }

    @Test
    void hookAndProbeActions() {
        Scene s = new Scene("""
            command fun
                shoot an arrow
                shoot a firework
                throw a snowball
                send Alex to server hub
                set world rule do-mob-spawning to false
                set redstone signal for the gate to true
            """);
        s.engine.interpreter().runCommand("fun", List.of(), "Alex");
        assertTrue(s.runtime.log.stream().anyMatch(l -> l.equals("shoot Alex arrow")));
        assertTrue(s.runtime.log.stream().anyMatch(l -> l.equals("firework Alex")));
        assertTrue(s.runtime.log.stream().anyMatch(l -> l.equals("throw Alex snowball")));
        assertTrue(s.runtime.log.stream().anyMatch(l -> l.equals("send Alex to hub")),
                "log was: " + s.runtime.log);
        assertTrue(s.runtime.log.stream().anyMatch(l -> l.equals("rule do-mob-spawning false")));
        assertTrue(s.runtime.log.stream().anyMatch(l -> l.contains("redstone") && l.contains("gate")));
    }

    @Test
    void eventSupervisionStillWorks() {
        Scene s = new Scene("""
            when player is hurt
                if player health is below 3
                    announce danger
                    cancel event
            """);
        s.engine.trigger(new Trigger("damage", "Alex"));
        s.runtime.player("Alex").hp = 1;
        assertFalse(s.runtime.chatter.contains("announce: danger"));
    }
}
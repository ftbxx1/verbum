package dev.verbum;

import dev.verbum.engine.ScriptEngine;
import dev.verbum.runtime.MockMcRuntime;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the big feature batch: new live variables (xp, saturation, air,
 * ping, team, difficulty, tps ...), new library actions (slots, swap, fly/attack
 * speed, armor slot, display names, broadcasts, coords effects) and the new
 * Paper event phrases (swap hands, bucket, raid, breed, advancement ...).
 */
class ModernFeatureBatchTest {

    private static final class Scene {
        final MockMcRuntime runtime;
        final ScriptEngine engine;
        Scene(String source) {
            runtime = new MockMcRuntime();
            engine = new ScriptEngine(runtime);
            engine.load(source, "modern-batch-test.vb");
        }
    }

    private static boolean logForest(MockMcRuntime r, String needle) {
        return r.log.stream().anyMatch(l -> l.contains(needle));
    }

    // ------------------------------------------------------------- actions

    @Test
    void slotAndPutPlaceItems() {
        Scene s = new Scene("""
            when player joins
                set slot 5 of player to diamond
                put emerald in slot 2 of player
            """);
        s.engine.trigger("join", "Steve");
        assertTrue(logForest(s.runtime, "slot Steve 5 diamond"));
        assertTrue(logForest(s.runtime, "slot Steve 2 emerald"));
    }

    @Test
    void swapHandsAndClearAirAllThere() {
        Scene s = new Scene("""
            when player joins
                swap hands
                set fly speed of player to 0.3
                set attack speed of player to 12
                set saturation of player to 8
                set air of player to 300
                clear effects of player
                set food level of player to 20
                set respawn point of player
            """);
        s.engine.trigger("join", "Steve");
        assertTrue(logForest(s.runtime, "swap hands Steve"));
        assertTrue(logForest(s.runtime, "fly speed Steve 0.3"));
        assertTrue(logForest(s.runtime, "attack speed Steve 12"));
        assertTrue(logForest(s.runtime, "saturation Steve 8"));
        assertTrue(logForest(s.runtime, "air Steve 300"));
        assertTrue(logForest(s.runtime, "clear effects Steve"));
        assertTrue(logForest(s.runtime, "set food Steve 20"));
        assertTrue(logForest(s.runtime, "respawn point Steve"));
    }

    @Test
    void equipmentAndMetaActions() {
        Scene s = new Scene("""
            when player joins
                set armor slot of player to diamond helmet on head
                set display name of player to Wizard
                set list name of player to Wizard
                set glow color of player to red
                set tab list header of player to Hello and World
                launch player 5
                feed fully
            """);
        s.engine.trigger("join", "Steve");
        assertTrue(logForest(s.runtime, "armor slot Steve head diamond helmet"));
        assertTrue(logForest(s.runtime, "display name Steve Wizard"));
        assertTrue(logForest(s.runtime, "list name Steve Wizard"));
        assertTrue(logForest(s.runtime, "glow color Steve red"));
        assertTrue(logForest(s.runtime, "tablist header Steve Hello | World"));
        assertTrue(logForest(s.runtime, "launch Steve 5"));
        assertTrue(s.runtime.food("Steve") >= 20, "feed fully fills food");
    }

    @Test
    void coordinateEffects() {
        Scene s = new Scene("""
            when player joins
                drop experience at 100 64 100
                drop item at 100 64 100 diamond
                fill region 0 64 0 to 10 70 10 with stone
            """);
        s.engine.trigger("join", "Steve");
        assertTrue(logForest(s.runtime, "xp orbs at world (100, 64, 100) 10"));
        assertTrue(logForest(s.runtime, "drop at world (100, 64, 100) diamond x1"));
        assertTrue(logForest(s.runtime, "fill world (0, 64, 0) to world (10, 70, 10) with stone"));
    }

    @Test
    void broadcastsAndMusic() {
        Scene s = new Scene("""
            when player joins
                play music disc cat for player
                silence sounds of player
                broadcast toast welcome!
            """);
        s.runtime.player("Steve");
        s.engine.trigger("join", "Steve");
        assertTrue(logForest(s.runtime, "music disc Steve cat"));
        assertTrue(logForest(s.runtime, "stop sounds Steve"));
        assertTrue(s.runtime.chatter.stream().anyMatch(c -> c.contains("welcome!")), () -> s.runtime.chatter.toString());
    }

    // ------------------------------------------------------------- live variables

    @Test
    void newLiveVariablesReadBack() {
        Scene s = new Scene("""
            when player joins
                if player's saturation is at least 5
                    announce sat-ok
                if player's air is at least 100
                    announce air-ok
                if player's ping is at least 1
                    announce ping-ok
                if server's tps is at least 15
                    announce tps-ok
                set out2 to world's day count
                tell player day %out2%
                set out3 to world's difficulty
                tell player diff %out3%
            """);
        s.engine.trigger("join", "Steve");
        assertTrue(s.runtime.chatter.contains("announce: sat-ok"));
        assertTrue(s.runtime.chatter.contains("announce: air-ok"));
        assertTrue(s.runtime.chatter.contains("announce: ping-ok"));
        assertTrue(s.runtime.chatter.contains("announce: tps-ok"));
        assertTrue(s.runtime.chatter.stream().anyMatch(c -> c.contains("day 0")), () -> s.runtime.chatter.toString());
        assertTrue(s.runtime.chatter.stream().anyMatch(c -> c.contains("diff normal")), () -> s.runtime.chatter.toString());
    }

    @Test
    void worldPossessiveDoesNotFallThroughToGlobal() {
        Scene s = new Scene("""
            when player joins
                set out1 to world's border
                if out1 equals 0
                    announce border-ok
            """);
        s.engine.trigger("join", "Steve");
        assertTrue(s.runtime.chatter.contains("announce: border-ok"));
    }

    // ------------------------------------------------------------- new event phrases

    @Test
    void newEventKindsMatchTheirPhrases() {
        Scene s = new Scene("""
            when player swaps hands
                announce E-swap
            when player empties a bucket
                announce E-bucket
            when player fills a bucket
                announce E-fill
            when a player changes gamemode
                announce E-gm
            when a player changes worlds
                announce E-world
            when a player tames
                announce E-tame
            when a player breeds
                announce E-breed
            when a raid finishes
                announce E-raid
            when a piston extends
                announce E-piston
            when a player gets an advancement
                announce E-adv
            when a player uses a totem
                announce E-totem
            when a player starts crafting
                announce E-craft
            """);
        assertTrue(match(s, "swap", "E-swap"));
        assertTrue(match(s, "bucketempty", "E-bucket"));
        assertTrue(match(s, "bucketfill", "E-fill"));
        assertTrue(match(s, "gamemodechange", "E-gm"));
        assertTrue(match(s, "worldchange", "E-world"));
        assertTrue(match(s, "tame", "E-tame"));
        assertTrue(match(s, "breed", "E-breed"));
        assertTrue(match(s, "raidwin", "E-raid"));
        assertTrue(match(s, "piston", "E-piston"));
        assertTrue(match(s, "advancement", "E-adv"));
        assertTrue(match(s, "totem", "E-totem"));
        assertTrue(match(s, "craftstart", "E-craft"));
    }

    @Test
    void reWiredKindsStillFire() {
        Scene s = new Scene("""
            when player gets kicked
                announce E-kick
            when player falls into void
                announce E-void
            when player catches fire
                announce E-ignite
            when player is hurt by
                announce E-hurt
            when player switches slot
                announce E-switch
            """);
        s.engine.trigger("kick", "Steve");
        assertTrue(s.runtime.chatter.contains("announce: E-kick"));
        assertTrue(match(s, "void", "E-void"));
        assertTrue(match(s, "ignite", "E-ignite"));
        assertTrue(match(s, "hurt", "E-hurt"));
        assertTrue(match(s, "switch", "E-switch"));
    }

    private static boolean match(Scene s, String kind, String marker) {
        s.runtime.chatter.clear();
        s.engine.trigger(kind, "Steve");
        return s.runtime.chatter.contains("announce: " + marker);
    }
}
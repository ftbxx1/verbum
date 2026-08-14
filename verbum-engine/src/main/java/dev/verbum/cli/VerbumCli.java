package dev.verbum.cli;

import dev.verbum.engine.ScriptEngine;
import dev.verbum.error.VerbumError;
import dev.verbum.interp.Trigger;
import dev.verbum.runtime.MockMcRuntime;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Command-line entry point for Verbum.
 *
 *   java -jar verbum-engine.jar check  <file>   -> parse and report
 *   java -jar verbum-engine.jar run    <file>   -> load + run a demo scenario
 *   java -jar verbum-engine.jar demo            -> run the acceptance test offline
 *   java -jar verbum-engine.jar help            -> usage
 */
public final class VerbumCli {

    public static void main(String[] args) {
        if (args.length == 0) { help(); return; }
        try {
            switch (args[0].toLowerCase()) {
                case "demo" -> runAcceptanceDemo();
                case "check" -> check(args.length > 1 ? args[1] : null);
                case "run" -> runFile(args.length > 1 ? args[1] : null);
                case "help", "-h", "--help" -> help();
                default -> { System.out.println("I do not know the command: " + args[0]); help(); }
            }
        } catch (VerbumError e) {
            System.out.println(e.pretty());
        } catch (Throwable t) {
            System.out.println("Something unexpected happened:\n  " + t);
        }
    }

    // ---------------------------------------------------------------- check

    private static void check(String file) {
        if (file == null) { System.out.println("I need a file name:  check <file>"); return; }
        ScriptEngine engine = new ScriptEngine(new MockMcRuntime());
        engine.loadFile(file);
        System.out.println("The file " + file + " is valid Verbum. No problems found.");
    }

    // ---------------------------------------------------------------- run

    private static void runFile(String file) {
        if (file == null) { System.out.println("I need a file name:  run <file>"); return; }
        MockMcRuntime runtime = new MockMcRuntime();
        ScriptEngine engine = new ScriptEngine(runtime);
        engine.loadFile(file);
        engine.onServerStart();
        simulate(engine);
        printLog(runtime, file);
    }

    // ---------------------------------------------------------------- acceptance demo

    private static void runAcceptanceDemo() {
        System.out.println("=== Verbum acceptance demo (offline mock world) ===\n");
        MockMcRuntime runtime = new MockMcRuntime();
        ScriptEngine engine = new ScriptEngine(runtime);

        String source = readResource("/scripts/acceptance.vb");
        engine.load(source, "acceptance.vb");
        engine.onServerStart();

        System.out.println("  Steve joins the world.");
        engine.trigger("join", "Steve");

        System.out.println("  Steve touches water -> should be killed.");
        engine.trigger("touch water", "Steve");

        engine.tick();

        System.out.println("  Steve collects 10 emeralds -> should win.");
        for (int i = 0; i < 10; i++) {
            engine.trigger(new Trigger("collect", "Steve").with("p", "emerald"));
            engine.tick();
        }

        System.out.println("  The boss dies -> everyone should get a dragon egg.");
        engine.trigger(new Trigger("boss death", "ZombieKing"));
        engine.tick();

        System.out.println();
        printLog(runtime, "acceptance.vb");

        boolean pass = true;
        pass &= require(runtime.log, "kill Steve", "touch water killed Steve");
        pass &= require(runtime.chatter, "announce: Player Wins", "winning announcement");
        boolean teleported = runtime.log.stream().anyMatch(l -> l.startsWith("teleport Steve"));
        if (!teleported) { pass = false; System.out.println("  FAIL: Steve was not teleported to victory area."); }
        boolean egg = runtime.log.stream().anyMatch(l -> l.contains("give Steve") && l.contains("dragon egg"));
        if (!egg) { System.out.println("  FAIL: Steve did not get a dragon egg."); }
        pass &= egg;
        boolean anyEgg = runtime.log.stream().anyMatch(l -> l.contains("dragon egg"));
        System.out.println("  " + (anyEgg ? "PASS" : "FAIL") + ": everyone got a dragon egg");

        System.out.println();
        System.out.println(pass ? "ACCEPTANCE TEST: PASS" : "ACCEPTANCE TEST: FAIL");
    }

    // ---------------------------------------------------------------- helpers

    private static void simulate(ScriptEngine engine) {
        // A small scenario the user can watch for any loaded file.
        engine.trigger("join", "Alex");
        engine.tick();
        engine.trigger("touch water", "Alex");
        engine.tick();
        engine.trigger(new Trigger("boss death", "ZombieKing"));
        engine.tick();
    }

    private static void printLog(MockMcRuntime runtime, String name) {
        System.out.println("--- runtime log for " + name + " ---");
        for (String l : runtime.log) System.out.println("  " + l);
        System.out.println("--- chat ---");
        for (String c : runtime.chatter) System.out.println("  " + c);
        System.out.println("--- player state ---");
        for (var e : runtime.players.entrySet()) {
            MockMcRuntime.MockPlayer p = e.getValue();
            System.out.println("  " + p.name + " hp=" + p.hp + " online=" + p.online
                    + " inv=" + p.inventory + " dim=" + p.dimension);
        }
    }

    private static boolean require(List<String> haystack, String needle, String what) {
        boolean ok = haystack.stream().anyMatch(l -> l.equalsIgnoreCase(needle)
                || l.toLowerCase().contains(needle.toLowerCase()));
        System.out.println("  " + (ok ? "PASS" : "FAIL") + ": " + what + "  [" + needle + "]");
        return ok;
    }

    private static String readResource(String path) {
        try (InputStream in = VerbumCli.class.getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("Missing resource " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void help() {
        System.out.println("""
                Verbum — a beginner-friendly English language for Minecraft

                Usage:
                  check  <file>      check a .vb / .mcscript file for problems
                  run    <file>      load and run a small demo scenario
                  demo               run the offline acceptance test
                  help               show this help

                Example:
                  java -jar verbum-engine.jar run game.mcscript""");
    }
}

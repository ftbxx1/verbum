package dev.verbum.api;

import dev.verbum.interp.Interpreter;
import java.util.List;

/**
 * A native command registered by an add-on, usable as <pre>command mycmd ...</pre>
 * or invoked via <pre>/mycmd</pre> from the server console.
 *
 * <pre>
 *   r.registerCommand(NativeCommand.of("broadcast",
 *       (it, words, line) -> { it.runtime().announce(it.focus()); }));
 * </pre>
 */
public interface NativeCommand {

    /** The command word (what follows  command  in a script, or the slash alias). */
    String word();

    /** Runs the command. {@code words} is everything after the command word. */
    void run(Interpreter interp, List<String> words, int line);

    /** Shortcut factory:  command("broadcast", (it, words, line) -> ...) */
    static NativeCommand of(String w, Runner r) {
        return new NativeCommand() {
            @Override public String word() { return w; }
            @Override public void run(Interpreter interp, List<String> words, int line) { r.run(interp, words, line); }
        };
    }

    @FunctionalInterface
    interface Runner { void run(Interpreter interp, List<String> words, int line); }
}
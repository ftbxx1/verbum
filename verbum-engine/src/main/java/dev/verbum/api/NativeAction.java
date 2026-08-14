package dev.verbum.api;

import dev.verbum.interp.Interpreter;

import java.util.ArrayList;
import java.util.List;

/**
 * A native action verb registered by an add-on. When a script says
 * <pre>  charge item</pre>
 * and "charge" is not a built-in verb or custom action, this runs instead.
 */
public interface NativeAction {

    String verb();

    /** Runs the action. {@code words} is everything after the verb. */
    void run(Interpreter interp, List<String> words, int line);

    /** Convenience factory:  action("charge", (it, words, line) -> ...) */
    static NativeAction of(String verb, Runner r) {
        return new NativeAction() {
            @Override public String verb() { return verb; }
            @Override public void run(Interpreter interp, List<String> words, int line) { r.run(interp, words, line); }
        };
    }

    /** Splits words on  "and"  at this depth level, like function arguments. */
    static List<List<String>> splitNaive(List<String> words) {
        List<List<String>> out = new ArrayList<>();
        List<String> cur = new ArrayList<>();
        for (String w : words) {
            if (w.equalsIgnoreCase("and") && !cur.isEmpty()) { out.add(new ArrayList<>(cur)); cur.clear(); }
            else cur.add(w);
        }
        if (!cur.isEmpty()) out.add(cur);
        return out;
    }

    @FunctionalInterface
    interface Runner { void run(Interpreter interp, List<String> words, int line); }
}
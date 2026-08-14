package dev.verbum.interp;

import dev.verbum.error.VerbumError;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Small helpers for reading numbers and lists out of raw Verbum words.
 */
public final class MathWords {

    private final Interpreter it;

    public MathWords(Interpreter it) { this.it = it; }

    /** Read a number from words: a plain number, a variable, or  random between A and B. */
    public double numberOf(List<String> words, int line) {
        if (words.isEmpty()) throw new VerbumError(line, "I expected a number here.");
        String joined = VariableStore.join(words);

        // random between 1 and 100
        if (joined.toLowerCase().startsWith("random") && joined.contains("between")) {
            int b = indexOfIgnoreCase(words, "between");
            int and = indexOfIgnoreCase(words, b + 1, "and");
            if (b >= 0 && and > b) {
                double lo = Double.parseDouble(words.get(b + 1).replace(",", ""));
                double hi = Double.parseDouble(words.get(and + 1).replace(",", ""));
                Random r = ThreadLocalRandom.current();
                if (lo == Math.floor(lo) && hi == Math.floor(hi)) {
                    return r.nextInt((int) hi - (int) lo + 1) + lo;
                }
                return lo + r.nextDouble() * (hi - lo);
            }
        }

        // single numeric token
        if (words.size() == 1) {
            String w = words.get(0).replace(",", "");
            try { return Double.parseDouble(w); } catch (NumberFormatException ignore) { }
        }

        // variable reference
        Object[] ref = VariableStore.resolve(words);
        VariableStore.Scope scope = (VariableStore.Scope) ref[0];
        String key = (String) ref[1];
        if (it.store().has(scope, key)) {
            return it.store().asNumber(it.store().get(scope, key), line);
        }
        // live values:  player's health, amount of all players, ...
        Object live = dev.verbum.interp.Actions.liveValue(it, scope, key);
        if (live != null) return it.store().asNumber(live, line);
        throw new VerbumError(line,
                "I expected a number but got:  " + joined + "\n" +
                "Try a number like  10,  or a stored value like  player's coins");
    }

    /** Read a list of values from words: online players, inventory, or a list variable. */
    public List<Object> listOf(List<String> words) {
        String raw = VariableStore.join(words);

        // A variable phrase (player's list, world names, {warps::*}, my items ...)
        // wins over the built-in player/inventory collections.
        boolean looksVar = raw.contains("'s") || raw.contains("s'") || raw.contains("::")
                || raw.startsWith("{")
                || (words.size() > 0 && List.of("world", "global", "temp", "player", "players")
                    .contains(words.get(0).toLowerCase().replace("'", "")));
        if (looksVar) {
            Object[] ref = VariableStore.resolve(words);
            VariableStore.Scope scope = (VariableStore.Scope) ref[0];
            String key = (String) ref[1];
            Object v = it.store().get(scope, key);
            if (v instanceof List<?> l) return new ArrayList<>(l);
            if (v != null) { List<Object> one = new ArrayList<>(); one.add(v); return one; }
            // fall through to built-in collections if the variable is empty
        }

        String joined = raw.toLowerCase();
        if (joined.contains("player") || joined.contains("online") || joined.contains("all")) {
            List<Object> out = new ArrayList<>();
            if (it.runtime() instanceof dev.verbum.runtime.MockMcRuntime mock) {
                out.addAll(mock.players.keySet());
            }
            return out;
        }
        if (joined.contains("inventory")) {
            List<Object> out = new ArrayList<>();
            if (it.runtime() instanceof dev.verbum.runtime.MockMcRuntime mock) {
                var p = mock.player(it.focus());
                out.addAll(p.inventory.keySet());
            }
            return out;
        }

        return new ArrayList<>();
    }

    private static int indexOfIgnoreCase(List<String> words, String target) {
        return indexOfIgnoreCase(words, 0, target);
    }

    private static int indexOfIgnoreCase(List<String> words, int from, String target) {
        for (int i = from; i < words.size(); i++) {
            if (words.get(i).equalsIgnoreCase(target)) return i;
        }
        return -1;
    }
}

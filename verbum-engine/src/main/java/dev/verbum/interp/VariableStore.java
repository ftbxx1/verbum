package dev.verbum.interp;

import dev.verbum.error.VerbumError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Verbum's variable store. Values are plain Java objects: Double, String,
 * Boolean, List, or Map. Variables live in one of four scopes.
 *
 *   player's X  -> per-player (keyed on the current focus player)
 *   world X     -> per-world / server-wide
 *   temp X      -> per action run (stack-local)
 *   X (none)    -> global
 */
public final class VariableStore {

    public enum Scope { GLOBAL, WORLD, PLAYER, TEMP }

    private final Map<String, Object> global = new HashMap<>();
    private final Map<String, Object> world = new HashMap<>();
    private final Map<String, Map<String, Object>> players = new HashMap<>();
    private final Map<String, Object> temp = new HashMap<>();

    private String focus = "";

    public void setFocus(String player) { this.focus = player == null ? "" : player; }
    public String focus() { return focus; }

    public void clearTemp() { temp.clear(); }

    // --------------------------------------------------------------------- storage

    private Map<String, Object> bucket(Scope s) {
        switch (s) {
            case GLOBAL: return global;
            case WORLD: return world;
            case TEMP: return temp;
            case PLAYER:
            default:
                return players.computeIfAbsent(focus.toLowerCase(), k -> new HashMap<>());
        }
    }

    public void set(Scope scope, String key, Object value) {
        String base = baseKey(key);
        String sub = subKey(key);
        if (sub == null) { bucket(scope).put(key, value); return; }
        if (sub.equals("*")) {
            bucket(scope).put(base, value instanceof List<?> l ? new ArrayList<>(l) : listOf(value));
            return;
        }
        if (isIndex(sub)) {
            List<Object> l = bucket(scope).containsKey(base) && bucket(scope).get(base) instanceof List<?> m
                    ? new ArrayList<>((List<?>) m) : new ArrayList<>();
            long idx = Long.parseLong(sub);
            if (idx < 1) throw new VerbumError("I could not use the list position " + sub + " - positions start at 1.");
            if (idx > 1000000) throw new VerbumError("The list position " + sub + " is too big - the maximum is 1000000.");
            while (l.size() < idx) l.add(null);
            l.set((int) (idx - 1), value);
            bucket(scope).put(base, l);
            return;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> m = bucket(scope).containsKey(base) && bucket(scope).get(base) instanceof Map<?, ?> mp
                ? (Map<String, Object>) mp : new HashMap<>();
        m.put(sub, value);
        bucket(scope).put(base, m);
    }

    public Object get(Scope scope, String key) {
        String base = baseKey(key);
        String sub = subKey(key);
        if (sub == null) return bucket(scope).get(key);
        if (sub.equals("*")) {
            Object v = bucket(scope).get(base);
            if (v instanceof List<?> l) return new ArrayList<>(l);
            if (v == null) return new ArrayList<>();
            List<Object> one = new ArrayList<>(); one.add(v); return one;
        }
        Object v = bucket(scope).get(base);
        if (isIndex(sub)) {
            if (v instanceof List<?> l) {
                long idx = Long.parseLong(sub);
                return idx >= 1 && idx <= l.size() ? l.get((int) (idx - 1)) : null;
            }
            return null;
        }
        if (v instanceof Map<?, ?> m) return m.get(sub);
        if (v instanceof List<?> l) {
            return l.stream().filter(o -> o != null && o.toString().equalsIgnoreCase(sub)).findFirst().orElse(null);
        }
        return null;
    }

    public boolean has(Scope scope, String key) {
        String base = baseKey(key);
        String sub = subKey(key);
        if (sub == null) return bucket(scope).containsKey(key);
        if (sub.equals("*")) return bucket(scope).get(base) instanceof List<?>;
        Object v = bucket(scope).get(base);
        if (isIndex(sub)) {
            if (v instanceof List<?> l) {
                long idx = Long.parseLong(sub);
                return idx >= 1 && idx <= l.size() && l.get((int) (idx - 1)) != null;
            }
            return false;
        }
        if (v instanceof Map<?, ?> m) return m.containsKey(sub);
        if (v instanceof List<?> l) return l.stream().anyMatch(o -> o != null && o.toString().equalsIgnoreCase(sub));
        return false;
    }

    /** Deletes a variable: whole keys, ::*  lists, numeric indexes or named entries. */
    public void removeVar(Scope scope, String key) {
        String base = baseKey(key);
        String sub = subKey(key);
        if (sub == null) { bucket(scope).remove(key); return; }
        Object v = bucket(scope).get(base);
        if (sub.equals("*")) { bucket(scope).remove(base); return; }
        if (isIndex(sub)) {
            if (v instanceof List<?> l) {
                List<Object> nl = new ArrayList<>(l);
                long idx = Long.parseLong(sub);
                if (idx >= 1 && idx <= nl.size()) nl.remove((int) (idx - 1));
                bucket(scope).put(base, nl);
            }
            return;
        }
        if (v instanceof Map<?, ?> m) {
            Map<String, Object> nm = new HashMap<>();
            for (Map.Entry<?, ?> e : m.entrySet()) nm.put(String.valueOf(e.getKey()), e.getValue());
            nm.remove(sub);
            bucket(scope).put(base, nm);
        }
    }

    /** The part left of the first  ::  in a list key, or the key itself if none. */
    private static String baseKey(String key) {
        int i = key.indexOf("::");
        return i < 0 ? key : key.substring(0, i);
    }

    /** The part after the first  :: , or null if the key has none. */
    private static String subKey(String key) {
        int i = key.indexOf("::");
        return i < 0 ? null : key.substring(i + 2);
    }

    private static boolean isIndex(String sub) {
        return sub.chars().allMatch(Character::isDigit);
    }

    private static List<Object> listOf(Object value) {
        List<Object> one = new ArrayList<>();
        if (value != null) one.add(value);
        return one;
    }

    public Map<String, Object> playerVars(String player) {
        return players.computeIfAbsent(player.toLowerCase(), k -> new HashMap<>());
    }

    // ------------------------------------------------------------- persistence

    /**
     * Exports the durable variables (global, world, per-player; not temp) as a
     * plain nested map fit for JSON / YAML stores.
     */
    public Map<String, Object> snapshot() {
        Map<String, Object> out = new LinkedHashMap<>();
        Map<String, Object> g = new LinkedHashMap<>(global);
        Map<String, Object> w = new LinkedHashMap<>(world);
        Map<String, Map<String, Object>> pl = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Object>> e : players.entrySet()) {
            pl.put(e.getKey(), new LinkedHashMap<>(e.getValue()));
        }
        out.put("global", g);
        out.put("world", w);
        out.put("players", pl);
        return out;
    }

    /** Replaces the durable variables with a map produced by {@link #snapshot()}. */
    @SuppressWarnings("unchecked")
    public void restore(Map<String, Object> data) {
        global.clear();
        world.clear();
        temp.clear();
        if (data == null) return;
        Object g = data.get("global");
        Object w = data.get("world");
        Object pl = data.get("players");
        if (g instanceof Map<?, ?> gm) global.putAll((Map<? extends String, ?>) gm);
        if (w instanceof Map<?, ?> wm) world.putAll((Map<? extends String, ?>) wm);
        if (pl instanceof Map<?, ?> pm) {
            for (Map.Entry<?, ?> e : pm.entrySet()) {
                String name = String.valueOf(e.getKey()).toLowerCase();
                if (e.getValue() instanceof Map<?, ?> playerVars) {
                    players.put(name, new HashMap<>(playerVars.size()));
                    for (Map.Entry<?, ?> pv : playerVars.entrySet()) {
                        players.get(name).put(String.valueOf(pv.getKey()), pv.getValue());
                    }
                }
            }
        }
    }

    // --------------------------------------------------------------------- parsing

    /**
     * Turns target words like {"player's","coins"} into a scope + key.
     * Supports "player's X", "players' X", "player X", "world X", "world's X",
     * "global X", "temp X", ideologically anything else is global "X".
     */
    public static Object[] resolve(List<String> words) {
        if (words.isEmpty()) {
            throw new VerbumError("I need a variable name, like  player's coins  or  highscore");
        }
        words = stripBraces(words);
        String first = words.get(0).toLowerCase().replace("'", "").replace("’", "");
        Scope scope;
        int from;
        switch (first) {
            case "player": case "players":
                scope = Scope.PLAYER; from = 1; break;
            case "world": case "worlds":
                scope = Scope.WORLD; from = 1; break;
            case "global":
                scope = Scope.GLOBAL; from = 1; break;
            case "temp": case "temporary":
                scope = Scope.TEMP; from = 1; break;
            default:
                scope = Scope.GLOBAL; from = 0; break;
        }
        if (from >= words.size()) {
            throw new VerbumError("I could not find the variable name after  " + words.get(0));
        }
        String key = join(words.subList(from, words.size())).toLowerCase();
        return new Object[]{ scope, key };
    }

    /**
     * Accepts Skript-style curly braces:  {coins}, {player's coins}, {warps::*}.
     * Braces come attached to words (the tokenizer only splits on spaces), so a
     * brace can land on the first word, the last word, or even a word in the
     * middle:  if {coins} is greater than 5  ->  the {coins} token holds both
     * braces. Stripping the leading  {  and trailing  }  from every word covers
     * all of those shapes.
     */
    public static List<String> stripBraces(List<String> words) {
        List<String> out = new ArrayList<>(words);
        for (int i = 0; i < out.size(); i++) {
            String w = out.get(i);
            if (w.length() > 1 && w.startsWith("{")) out.set(i, w.substring(1));
        }
        for (int i = 0; i < out.size(); i++) {
            String w = out.get(i);
            if (w.length() > 1 && w.endsWith("}")) out.set(i, w.substring(0, w.length() - 1));
        }
        return out;
    }

    public double asNumber(Object v, int line) {
        if (v instanceof Double d) return d;
        if (v instanceof Integer i) return i;
        if (v instanceof Long l) return l;
        if (v instanceof String s) {
            try { return Double.parseDouble(s); }
            catch (NumberFormatException e) { throw new VerbumError(line, "I tried to use \"" + s + "\" as a number, but it is text."); }
        }
        if (v instanceof Boolean b) return b ? 1 : 0;
        throw new VerbumError(line, "That value is not a number.");
    }

    public static String asText(Object v) {
        if (v == null) return "";
        if (v instanceof Double d) {
            double dv = d;
            return dv == Math.rint(dv) ? String.valueOf((long) dv) : String.valueOf(dv);
        }
        return String.valueOf(v);
    }

    public static boolean asBool(Object v) {
        if (v == null) return false;
        if (v instanceof Boolean b) return b;
        if (v instanceof Double d) return d != 0;
        if (v instanceof String s) return s.equalsIgnoreCase("true") || s.equalsIgnoreCase("yes");
        return false;
    }

    public static String join(List<String> words) { return String.join(" ", words); }
}

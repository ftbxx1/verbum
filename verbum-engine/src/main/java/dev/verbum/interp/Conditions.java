package dev.verbum.interp;

import dev.verbum.error.VerbumError;
import dev.verbum.runtime.McRuntime;

import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates Verbum conditions.
 *
 * Conditions are plain words connected by  and / or / not. Each atomic predicate
 * is matched against either the currently fired Minecraft event (for  when  blocks)
 * or against live questions asked of the runtime (has item, is in area, health,
 * flags, weather, time, variables ...).
 */
public final class Conditions {

    private Conditions() {}

    /** A detected event situation inside condition words. */
    private record Situation(String kind, String param) {}

    public static boolean eval(List<String> words, Interpreter it, boolean strict) {
        // Skript-style  {coins} / {player's gold}  variable names
        words = VariableStore.stripBraces(words);
        // split into or-clauses
        List<List<String>> ors = split(words, "or");
        boolean any = false;
        for (List<String> orClause : ors) {
            if (evalAnd(orClause, it, strict)) { any = true; break; }
        }
        return any;
    }

    private static boolean evalAnd(List<String> clause, Interpreter it, boolean strict) {
        List<List<String>> ands = split(clause, "and");
        for (List<String> seg : ands) {
            if (!evalTerm(seg, it, strict)) return false;
        }
        return true;
    }

    /** Whether a condition is primarily an event ("touches water", "joins"). */
    public static boolean eventStyle(List<String> words, Interpreter it) {
        return eventOf(words, it) != null;
    }

    private static boolean evalTerm(List<String> seg, Interpreter it, boolean strict) {
        // strip  not  tokens, toggling negation (handles "player is not holding X")
        boolean neg = false;
        List<String> rem = new ArrayList<>();
        for (String w : seg) {
            if (w.equalsIgnoreCase("not")) neg = !neg;
            else rem.add(w);
        }
        boolean base = evalCore(rem, it, strict, eventOf(rem, it));
        return neg != base;
    }

    private static boolean evalCore(List<String> words, Interpreter it, boolean strict, Situation ev) {
        // Event-driven predicate: true only when the matching event just fired.
        if (ev != null) {
            Trigger t = it.currentTrigger();
            if (t == null) return false;
            String tp = t.param("p");
            if (tp != null && tp.startsWith("/")) tp = tp.substring(1);
            boolean kind = ev.kind().equals(t.kind);
            boolean param = ev.param() == null || (tp != null && tp.equalsIgnoreCase(ev.param()));
            return kind && param;
        }

        McRuntime r = it.runtime();
        String focus = it.focus();
        String s = join(words).toLowerCase();

        // event-cancelled:  when ... cancel event  ->  later handlers see  event is cancelled
        if (s.equals("event cancelled") || s.equals("event is cancelled")
                || s.equals("the event cancelled") || s.equals("the event is cancelled")) {
            return it.isEventCancelled();
        }
        if (s.equals("event not cancelled") || s.equals("event wasn't cancelled")
                || s.equals("the event wasn't cancelled")) {
            return !it.isEventCancelled();
        }

        // permission:  player has permission verbum.vip   (checked before the
        // generic "has item" branch so the word "has" doesn't eat it)
        if (containsWord(words, "permission") || containsWord(words, "perms")) {
            int p = indexOf(words, "permission") >= 0 ? indexOf(words, "permission") : indexOf(words, "perms");
            String perm = join(words.subList(p + 1, words.size()));
            return r.hasPermission(focus, perm.isEmpty() ? "verbum.use" : perm);
        }

        // potion / effect held
        if (s.contains("effect") || s.contains("potion")) {
            int idx = containsWord(words, "effect") ? indexOf(words, "effect") : indexOf(words, "potion");
            List<String> eff = words.subList(idx + 1, words.size());
            return r.hasEffect(focus, join(eff));
        }

        // coordinate comparisons: above y 60 / below y 10
        Boolean coord = coordCompare(words, it);
        if (coord != null) return coord;

        // number of X is N / equals N   (live counts like  number of all players)
        String lc = s.toLowerCase();
        if (lc.startsWith("number of ") || lc.startsWith("size of ")
                || lc.startsWith("length of ") || lc.startsWith("amount of ")) {
            int is = indexOf(words, "is");
            if (is < 0) is = indexOf(words, "equals");
            if (is > 0 && is == words.size() - 2) {
                try {
                    double right = Double.parseDouble(words.get(is + 1).replace(",", ""));
                    double left = resolveLeft(words.subList(0, is), it);
                    return left == right;
                } catch (NumberFormatException ignore) { }
            }
        }

        // ---- scoreboard library ------------------------------------------------------
        // score of X is at least Y  |  score of X is less than Y  |  score of X equals Y
        int scoreIdx = indexOf(words, "score");
        if (scoreIdx >= 0) {
            int si = -1;
            for (int i = scoreIdx + 1; i < words.size(); i++) {
                if (words.get(i).equalsIgnoreCase("is") || words.get(i).equalsIgnoreCase("equals")) { si = i; break; }
            }
            if (si > scoreIdx + 1) {
                String objective = join(words.subList(scoreIdx + 1, si)).replaceAll("(?i)^(the|for|of)\\s+", "").trim();
                double want = 0;
                for (int i = si + 1; i < words.size(); i++) {
                    String w = words.get(i).replace(",", "").replace("%", "");
                    if (isNumber(w)) { want = Double.parseDouble(w); break; }
                }
                double have = r.score(objective, focus);
                if (s.contains("more than")) return have > want;
                if (s.contains("less than")) return have < want;
                if (s.contains("at least")) return have >= want;
                if (s.contains("at most")) return have <= want;
                return have == want;
            }
        }

        // numeric / variable comparison: at least, more than, less than, ...
        // Live values are handled here BEFORE compare() so the word "kills" or
        // "distance" is resolved from the world, not treated as a plain variable.
        Boolean liveCmp = liveCompare(words, it, r, focus, s);
        if (liveCmp != null) return liveCmp;

        Boolean cmp = compare(words, it);
        if (cmp != null) return cmp;

        // player has N item
        int has = indexOf(words, "has");
        if (has >= 0) {
            int i = has + 1;
            double count = 1;
            if (i < words.size() && isNumber(words.get(i).replace(",", ""))) {
                count = Double.parseDouble(words.get(i).replace(",", ""));
                i++;
            }
            String item = join(words.subList(i, words.size()));
            if (item.isEmpty()) item = "item";
            if (r.hasItem(focus, item, count)) return true;
            // Also accept a stored player variable of the same name (friendliest meaning).
            if (it.store().has(VariableStore.Scope.PLAYER, item)) {
                double v = it.store().asNumber(it.store().get(VariableStore.Scope.PLAYER, item), 0);
                return v >= count;
            }
            return false;
        }

        // holding
        int hold = indexOf(words, "holding");
        if (hold >= 0) {
            String item = join(words.subList(hold + 1, words.size()));
            return r.isHolding(focus, item.isEmpty() ? "item" : item);
        }

        // list / string contains:  player's list contains diamond
        int contain = indexOf(words, "contains");
        if (contain >= 0) {
            List<String> left = words.subList(0, contain);
            String right = join(words.subList(contain + 1, words.size()));
            Object[] ref = VariableStore.resolve(left);
            Object v = it.store().get((VariableStore.Scope) ref[0], (String) ref[1]);
            if (v instanceof List<?> l) {
                return l.stream().anyMatch(o -> o.toString().equalsIgnoreCase(right));
            }
            return v != null && v.toString().toLowerCase().contains(right.toLowerCase());
        }

        // is empty / is set:  if {list::*} is empty   |   if {warps::*} is set
        int isSetIdx = indexOf(words, "is");
        if (isSetIdx > 0) {
            List<String> right = words.subList(isSetIdx + 1, words.size());
            if (containsWord(right, "empty")) {
                Object[] ref = VariableStore.resolve(words.subList(0, isSetIdx));
                VariableStore.Scope sc = (VariableStore.Scope) ref[0];
                String key = (String) ref[1];
                Object v = it.store().get(sc, key);
                return v == null || (v instanceof List<?> l && l.isEmpty())
                        || (v instanceof String str && str.isEmpty());
            }
            if (containsWord(right, "set")) {
                Object[] ref = VariableStore.resolve(words.subList(0, isSetIdx));
                return it.store().has((VariableStore.Scope) ref[0], (String) ref[1]);
            }
        }

        // chance of N percent (random)
        int chance = indexOf(words, "chance");
        if (chance >= 0) {
            for (String w : words) {
                if (isNumber(w.replace("%", ""))) {
                    double pct = Double.parseDouble(w.replace("%", ""));
                    return Math.random() * 100 < pct;
                }
            }
        }

        // is op / alive / dead   (permission is handled near the top of evalCore)
        if (containsWord(words, "op") && !containsWord(words, "stop") && !containsWord(words, "drop")
                && !containsWord(words, "shop") && !containsWord(words, "top")) {
            return r.isOp(focus);
        }
        int aliveIdx = -1;
        for (int i = 0; i < words.size(); i++) {
            String w = words.get(i);
            if (w.equalsIgnoreCase("alive")) { aliveIdx = i; break; }
        }
        if (aliveIdx >= 0) return r.playerAlive(focus);
        if (containsWord(words, "dead")) return !r.playerAlive(focus);

        // player is in team X   /   player is not in any team
        if (containsWord(words, "team") && containsWord(words, "in")) {
            if (s.contains("any team")) return r.teamMembers("all").isEmpty();
            String t = join(words.subList(indexOf(words, "in") + 1, words.size()))
                    .replaceFirst("(?i)^team\\s+", "").trim();
            return t.isEmpty() ? false : r.teamMembers(t).stream().anyMatch(x -> x.equalsIgnoreCase(focus));
        }

        // muted
        if (containsWord(words, "muted")) return r.isMuted(focus);

        // dimensions
        if (s.contains("nether")) return r.dimension(focus).equalsIgnoreCase("nether") || r.isIn(focus, "the nether");
        if (s.contains("the end") || s.contains("end dimension")) return r.dimension(focus).equalsIgnoreCase("the end") || r.isIn(focus, "the end");
        if (s.contains("overworld")) return r.dimension(focus).equalsIgnoreCase("overworld") || r.isIn(focus, "overworld");
        int dimIdx = indexOf(words, "dimension");
        if (dimIdx >= 0) {
            String d = join(words.subList(dimIdx + 1, words.size()));
            return d.isEmpty() ? false : r.dimension(focus).equalsIgnoreCase(d);
        }

        // exact clock times push past the generic day/night check
        if (s.contains("noon")) return r.timeOfDay().equalsIgnoreCase("noon");
        if (s.contains("midnight")) return r.timeOfDay().equalsIgnoreCase("midnight");

        // quests
        if (containsWord(words, "quest")) {
            int q = indexOf(words, "quest");
            List<String> tail = words.subList(Math.min(q + 1, words.size()), words.size());
            int stop = -1;
            for (int i = 0; i < tail.size(); i++) {
                String w = tail.get(i);
                if (w.equalsIgnoreCase("is") || w.equalsIgnoreCase("complete")
                        || w.equalsIgnoreCase("completes") || w.equalsIgnoreCase("done")
                        || w.equalsIgnoreCase("progress")) { stop = i; break; }
            }
            String name = (stop >= 0 ? join(tail.subList(0, stop)) : join(tail)).trim();
            if (name.isEmpty()) name = "main";
            if (s.contains("complete") || s.contains("done")) return r.questDone(name);
            if (s.contains("progress") && s.contains("at least")) {
                double want = 0;
                for (String w : words) if (isNumber(w.replace(",", ""))) want = Double.parseDouble(w.replace(",", ""));
                return r.questProgress(name) >= want;
            }
            return r.questProgress(name) > 0;
        }

        // named flags:  flag safe-zone is set   |   flag pvp is off
        if (containsWord(words, "flag")) {
            int f = indexOf(words, "flag");
            String name = join(words.subList(f + 1, words.size()))
                    .replaceAll("(?i)^(the\\s+|is\\s+|named\\s+)", "").replace("  ", " ").trim();
            if (s.contains("is set") || s.contains("set to true")) return r.getFlag(name);
            if (s.contains("off") || s.contains("not set")) return !r.getFlag(name);
            return r.getFlag(name);
        }

        // gamemode
        if (containsWord(words, "gamemode") || containsWord(words, "game mode")) {
            for (String g : new String[]{"creative", "survival", "adventure", "spectator"}) {
                if (s.contains(g)) return r.gamemode(focus).equalsIgnoreCase(g);
            }
        }

        // body sensors that are easy to confuse with the generic "in" branch
        if (s.contains("swimming")) return r.isSwimming(focus);
        if (s.contains("gliding") || s.contains("elytra")) return r.isGliding(focus);
        if (s.contains("underwater") || s.contains("in water")) return r.isInWater(focus);
        if (s.contains("falling")) return r.isFalling(focus);
        if (s.contains("climbing")) return r.isClimbing(focus);
        int wearIdx = indexOf(words, "wearing");
        if (wearIdx >= 0) {
            String armor = join(words.subList(wearIdx + 1, words.size()))
                    .replaceAll("(?i)^(a|an|the|armor|armour)\\s+", "").trim();
            return r.isWearing(focus, armor.isEmpty() ? "anything" : armor);
        }

        // environment extras: lava / bed / open sky
        if (s.contains("lava")) return r.isInLava(focus);
        if (s.contains("bed")) return r.isInBed(focus);
        if (s.contains("open sky") || (s.contains("sky") && s.contains("under"))) return r.isUnderSky(focus);

        // weapon type:  player weapon is diamond sword
        int wep = indexOf(words, "weapon");
        if (wep >= 0) {
            int cutW = indexOf(words, "is") >= 0 ? indexOf(words, "is") : indexOf(words, "equals");
            if (cutW > wep && cutW + 1 < words.size()) {
                String wname = join(words.subList(cutW + 1, words.size()))
                        .replaceAll("(?i)^(a|an|the)\\s+", "").trim();
                return wname.isEmpty() || r.weapon(focus).toLowerCase().contains(wname.toLowerCase());
            }
        }

        // is in place / vehicle
        int inIdx = indexOf(words, "in");
        if (inIdx >= 0) {
            String place = join(words.subList(inIdx + 1, words.size()));
            if (containsWord(words, "vehicle")) return r.isInVehicle(focus);
            if (place.isEmpty()) return false;
            return r.isIn(focus, place);
        }

        // weather & time
        if (s.contains("nighttime") || s.contains("night")) {
            if (containsWord(words, "day")) return false;
            return r.isNight();
        }
        if (s.contains("daytime") || (s.contains("day") && !s.contains("today"))) {
            return r.isDay();
        }
        if (s.contains("rain") || s.contains("raining")) return r.isRain();
        if (s.contains("storm")) return r.isStorm();
        if (s.contains("thunder") || s.contains("thundering")) return r.isStorm();
        if (s.contains("snow")) return false;

        // body flags
        if (s.contains("sprinting")) return r.isSprinting(focus);
        if (s.contains("sneaking") || s.contains("crouching")) return r.isSneaking(focus);
        if (s.contains("airborne") || s.contains("in the air")) return !r.isOnGround(focus);
        if (s.contains("on the ground") || (s.contains("on ground"))) return r.isOnGround(focus);
        if (s.contains("flying")) return r.isFlying(focus);
        if (s.contains("burning") || s.contains("on fire")) return r.isBurning(focus);
        if (s.contains("poisoned")) return r.isPoisoned(focus);

        // name checks
        int nameIdx = containsWord(words, "name") ? indexOf(words, "name") : -1;
        if (nameIdx >= 0 && focus != null) {
            String fname = focus.toLowerCase();
            List<String> rest = words.subList(nameIdx + 1, words.size());
            if (containsWord(rest, "contains")) {
                int c = indexOf(rest, "contains");
                String sub = join(rest.subList(c + 1, rest.size()));
                return fname.contains(sub.toLowerCase());
            }
            if (containsWord(rest, "starts") || containsWord(rest, "begins")) {
                int c = indexOf(rest, "with");
                String sub = c >= 0 ? join(rest.subList(c + 1, rest.size())) : "";
                return fname.startsWith(sub.toLowerCase());
            }
            if (containsWord(rest, "ends")) {
                int c = indexOf(rest, "with");
                String sub = c >= 0 ? join(rest.subList(c + 1, rest.size())) : "";
                return fname.endsWith(sub.toLowerCase());
            }
        }

        // standing in rain/etc
        if (s.contains("standing") && s.contains("rain")) return r.isRain();

        // boss at half health
        if (s.contains("half") && s.contains("boss")) return r.isBossHalfHealth("boss");

        // plain  X is 5  where X is a stored variable
        int isIdx = indexOf(words, "is");
        if (isIdx > 0 && isIdx == words.size() - 2) {
            List<String> left = words.subList(0, isIdx);
            List<String> right = words.subList(isIdx + 1, words.size());
            Object[] ref = VariableStore.resolve(left);
            VariableStore.Scope sc = (VariableStore.Scope) ref[0];
            String key = (String) ref[1];
            if (it.store().has(sc, key)) {
                double l = it.store().asNumber(it.store().get(sc, key), 0);
                double rgt = new MathWords(it).numberOf(right, 0);
                return l == rgt;
            }
        }

        // add-on conditions
        for (dev.verbum.api.NativeCondition nc : it.nativeConditions()) {
            if (nc.matches(words)) return nc.eval(it, words);
        }

        if (strict) {
            throw new VerbumError(
                    "I do not understand this condition:  " + s + "\n\n" +
                    "Some conditions I know:\n" +
                    "  player has 5 diamonds\n" +
                    "  player is holding a torch\n" +
                    "  player health is below 5\n" +
                    "  player is in the nether\n" +
                    "  it is nighttime\n" +
                    "  player's coins are at least 100\n" +
                    "  player is sprinting\n" +
                    "  server has more than 10 players online\n" +
                    "  event is cancelled\n" +
                    "Join ideas with  and  or  not.");
        }
        return false; // non-strict (event context): simply doesn't fire
    }

    // ------------------------------------------------------- event extraction

    private static Situation eventOf(List<String> words, Interpreter it) {
        String s = join(words).toLowerCase();

        // Numeric / state sentences are NOT events:  score of kills is at least 8,
        // player health is below 5, X equals 3, player has 5 kills ... The word
        // "kills" inside a stat check would otherwise look like a kill event.
        boolean numericAfterHas = false;
        int hasIdx = indexOf(words, "has");
        if (hasIdx >= 0) for (int i = hasIdx + 1; i < words.size(); i++)
            if (isNumber(words.get(i).replace(",", ""))) numericAfterHas = true;
        if (s.contains(" at least ") || s.contains(" more than ") || s.contains(" less than ")
                || s.contains(" at most ") || s.contains(" greater than ") || s.contains(" lower than ")
                || s.contains(" score") || s.startsWith("score") || s.contains(" equals ")
                || s.contains(" equals") || s.contains(" is exactly ") || numericAfterHas) {
            return null;
        }

        // --- connection / chat -----------------------------------------------
        if (s.contains("quits") || s.contains("leaves the server") || s.contains("logs out")
                || s.contains("goes offline") || s.contains("disconnects")) return new Situation("quit", null);
        if (s.contains("says") || s.contains("talks") || s.contains("chats") || s.contains("types")) {
            int i = Math.max(indexOf(words, "says"), Math.max(indexOf(words, "talks"),
                    Math.max(indexOf(words, "chats"), indexOf(words, "types"))));
            return new Situation("chat", i >= 0 && i + 1 < words.size() ? join(words.subList(i + 1, words.size())) : null);
        }
        if (s.contains("teleports to") || s.contains("warps to")) {
            int i = indexOf(words, "teleports") >= 0 ? indexOf(words, "teleports") : indexOf(words, "warps");
            return new Situation("teleport", i >= 0 && i + 2 < words.size() ? join(words.subList(i + 1, words.size())) : null);
        }
        if (s.contains("walks on lava")) return new Situation("walk on lava", null);
        if (s.contains("touches water") || s.contains("touch water")) return new Situation("touch water", null);
        if (s.contains("touches lava") || s.contains("touch lava")) return new Situation("touch lava", null);
        if (s.contains("touches fire") || s.contains("touch fire")) return new Situation("touch fire", null);
        if (s.contains("is struck by lightning") || s.contains("gets struck by lightning")) return new Situation("lightning", null);
        if (s.contains("falls into void") || s.contains("falls off the void")) return new Situation("void", null);

        // --- movement ----------------------------------------------------------
        if (s.contains("starts moving") || s.contains("starts walking")) return new Situation("move", null);
        if (s.contains("moves to") || s.contains("walks into") || s.contains("steps on")
                || s.contains("moves into")) {
            int i = Math.max(indexOf(words, "moves"), Math.max(indexOf(words, "walks"),
                    Math.max(indexOf(words, "steps"), indexOf(words, "steps"))));
            return new Situation("move", i >= 0 && i + 2 <= words.size()
                    ? join(words.subList(i + 1, words.size())) : null);
        }
        if (s.contains("starts sprinting") || s.contains("sprints")) return new Situation("sprint", null);
        if (s.contains("starts swimming") || s.contains("swims")) return new Situation("swim", null);
        if (s.contains("jumps")) return new Situation("jump", null);
        if (s.contains("falls from") || s.contains("starts falling")) return new Situation("fall", null);
        if (s.contains("drowns") || s.contains("starts drowning")) return new Situation("drown", null);
        if (s.contains("flies into") || s.contains("toggles flying")) return new Situation("toggleflight", null);
        if (s.contains("toggles sneak") || s.contains("starts sneaking")) return new Situation("togglesneak", null);
        if (s.contains("rides") || s.contains("mounts")) return new Situation("ride", null);
        if (s.contains("dismounts") || s.contains("gets off")) return new Situation("dismount", null);
        if (s.contains("goes to bed") || s.contains("sleeps")) return new Situation("sleep", null);
        if (s.contains("wakes up")) return new Situation("wake", null);

        // --- body & health ------------------------------------------------------
        if (s.contains("gets hurt") || s.contains("takes damage") || s.contains("is hurt by")) {
            return new Situation("hurt", null);
        }
        if (s.contains("catches fire") || s.contains("starts burning")) return new Situation("ignite", null);
        if (s.contains("gets frozen") || s.contains("freezes")) return new Situation("freeze", null);
        if (s.contains("heals") || s.contains("regenerates")) return new Situation("heal", null);
        if (s.contains("drinks")) {
            int i = indexOf(words, "drinks");
            return new Situation("consume", i >= 0 && i + 1 < words.size() ? join(words.subList(i + 1, words.size())) : null);
        }

        // --- projectiles (checked before generic  hits a / strikes  combat rules) ---
        if (s.contains("a projectile hits") || s.contains("his projectile hits")
                || s.contains("the projectile hits") || s.contains("projectile hits")) {
            return new Situation("projectilehit", null);
        }

        // --- combat ---------------------------------------------------------------
        if (s.contains("boss dies") || s.contains("boss death")) return new Situation("boss death", null);
        if (s.contains("boss spawn")) return new Situation("boss spawn", null);
        if (s.contains("mob dies")) {
            int i = indexOf(words, "dies");
            return new Situation("mob death", i >= 2 ? join(words.subList(0, i)) : null);
        }
        if (s.contains("gets killed by") || s.contains("is slain by")) return new Situation("death", null);
        if (s.contains("kills")) {
            return new Situation("kill", null);
        }
        if (s.contains("attacks") || s.contains("strikes") || s.contains("hits a")) return new Situation("hit", null);
        if (s.contains("shoots") || s.contains("fires a")) return new Situation("shoot", null);
        if (s.contains("gets shot") || s.contains("is hit by an arrow")) return new Situation("arrow", null);
        if (s.contains("blocks")) return new Situation("block", null);
        if (s.contains("explodes near") || s.contains("a creeper explodes")) return new Situation("explosion", null);
        if (s.contains("wins game")) return new Situation("win", null);
        if (s.contains("loses game")) return new Situation("lose", null);
        if (s.contains("levels up")) return new Situation("levelup", null);
        if (s.contains("gains experience") || s.contains("gets xp")) return new Situation("xp", null);

        // --- interaction ------------------------------------------------------------
        if (s.contains("right click")) return new Situation("rightclick", null);
        if (s.contains("left click")) return new Situation("leftclick", null);
        if (s.contains("middle click")) return new Situation("middleclick", null);
        if (s.contains("uses item") || s.contains("uses a item") || s.contains("uses an item")) return new Situation("use", null);
        if (s.contains("trades")) return new Situation("trade", null);
        if (s.contains("shears")) return new Situation("shear", null);
        if (s.contains("milks")) return new Situation("milk", null);
        if (s.contains("right clicks on a") || s.contains("right clicks the") || s.contains("right clicks a")) {
            int r = indexOf(words, "clicks");
            return new Situation("rightclick", r >= 0 && r + 3 < words.size() ? join(words.subList(r + 2, words.size())) : null);
        }
        if (s.contains("left clicks on a") || s.contains("left clicks the") || s.contains("left clicks a")) {
            int l = indexOf(words, "clicks");
            return new Situation("leftclick", l >= 0 && l + 3 < words.size() ? join(words.subList(l + 2, words.size())) : null);
        }
        if (s.contains("presses a button")) return new Situation("button", null);
        if (s.contains("flips a lever")) return new Situation("lever", null);
        if (s.contains("jumps")) return new Situation("jump", null);
        if (s.contains("lands")) return new Situation("land", null);
        if (s.contains("starts sprinting")) return new Situation("sprint", null);
        if (s.contains("stops sprinting")) return new Situation("unsprint", null);
        if (s.contains("starts sneaking")) return new Situation("sneak", null);
        if (s.contains("stops sneaking")) return new Situation("unsneak", null);
        if (s.contains("goes to bed") || s.contains("goes to sleep")) return new Situation("sleep", null);
        if (s.contains("wakes up")) return new Situation("wake", null);
        if (s.contains("it starts raining") || s.contains("rain starts")) return new Situation("rainstart", null);
        if (s.contains("storm starts") || s.contains("thunder starts")) return new Situation("stormstart", null);
        if (s.contains("thunder strikes")) return new Situation("thunder", null);
        if (s.contains("day starts") || s.contains("sunrise")) return new Situation("day", null);
        if (s.contains("night starts") || s.contains("sunset")) return new Situation("night", null);
        if (s.contains("takes fall damage")) return new Situation("fall", null);
        if (s.contains("starves")) return new Situation("starve", null);
        if (s.contains("takes void damage") || s.contains("falls into the void")) return new Situation("void", null);
        if (s.contains("gets kicked")) return new Situation("kick", null);
        if (s.contains("gets banned")) return new Situation("ban", null);
        if (s.contains("first joins")) return new Situation("firstjoin", null);
        if (s.contains("regains health") || (s.contains("heals") && !s.contains("heals a"))) return new Situation("heal", null);

        if (s.contains("opens a")) return new Situation("open", join(words.subList(indexOf(words, "opens") + 2, words.size())));
        if (s.contains("closes a")) return new Situation("close", join(words.subList(indexOf(words, "closes") + 2, words.size())));
        if (s.contains("enchants")) return new Situation("enchant", null);
        if (s.contains("fishes") || s.contains("reels")) return new Situation("fish", null);
        if (s.contains("tills") || s.contains("plants")) return new Situation("plant", null);
        if (s.contains("harvests") || s.contains("farms")) return new Situation("harvest", null);
        if (s.contains("starts playing")) return new Situation("start", null);
        if (s.contains("completes")) return new Situation("complete", null);
        if (s.contains("primes") || s.contains("lights a tnt")) return new Situation("prime", null);

        // --- more add-on events ------------------------------------------------------------
        if (s.contains("enters a portal") || s.contains("enters the portal") || s.contains("uses a portal")) return new Situation("portal", null);
        if (s.contains("exits a portal") || s.contains("exits the portal")) return new Situation("portalexit", null);
        if (s.contains("gets poisoned")) return new Situation("poison", null);
        if (s.contains("brews") || s.contains("alchemy")) return new Situation("brew", null);
        if (s.contains("is damaged by") || s.contains("gets damaged by") || s.contains("gets hurt by")) return new Situation("hurt", null);
        if (s.contains("consumes") || s.contains("drinks a potion")) return new Situation("consume", null);
        if (s.contains("burns to death") || s.contains("burnt to death")) return new Situation("fire", null);
        if (s.contains("drowns")) return new Situation("drown", null);

        // --- expansion batch: equipment, animals, crafting, raiding -----------------
        if (s.contains("swaps hands") || s.contains("switches his hands")) return new Situation("swap", null);
        if (s.contains("empties a bucket") || s.contains("pours out a bucket") || s.contains("pours a bucket")) return new Situation("bucketempty", null);
        if (s.contains("fills a bucket") || s.contains("collects water in a bucket")) return new Situation("bucketfill", null);
        if (s.contains("damages an item") || s.contains("damages his item") || s.contains("wears out an item")) return new Situation("itemdamage", null);
        if (s.contains("breaks a tool") || s.contains("breaks his tool") || s.contains("breaks an item")
                || s.contains("breaks a sword") || s.contains("breaks a pickaxe") || s.contains("breaks an axe")) return new Situation("itembreak", null);
        if (s.contains("clicks in a inventory") || s.contains("clicks in his inventory") || s.contains("clicks a slot")) return new Situation("inventoryclick", null);
        if (s.contains("a piston extends") || s.contains("a piston pushes")) return new Situation("piston", null);
        if (s.contains("a piston retracts") || s.contains("a piston pulls")) return new Situation("pistonretract", null);
        if (s.contains("plays a note") || s.contains("plays a music note") || s.contains("a note plays")
                || s.contains("plays the noteblock")) return new Situation("note", null);
        if (s.contains("starts a raid") || s.contains("triggers a raid") || s.contains("a raid starts")) return new Situation("raid", null);
        if (s.contains("finishes a raid") || s.contains("wins a raid") || s.contains("a raid finishes")
                || s.contains("a raid ends")) return new Situation("raidwin", null);
        if (s.contains("changes gamemode") || s.contains("changes game mode") || s.contains("switches gamemode")) return new Situation("gamemodechange", null);
        if (s.contains("changes worlds") || s.contains("travels between worlds") || s.contains("changes world")) return new Situation("worldchange", null);
        if (s.contains("breeds") || s.contains("breeds animals")) return new Situation("breed", null);
        if (s.contains("tames") || s.contains("tames a pet") || s.contains("tames an animal")) return new Situation("tame", null);
        if (s.contains("gets an advancement") || s.contains("earns an advancement")
                || s.contains("achieves an advancement") || s.contains("completes an advancement")) return new Situation("advancement", null);
        if (s.contains("uses a totem") || s.contains("pops a totem") || s.contains("his totem activates")) return new Situation("totem", null);
        if (s.contains("edits a book") || s.contains("writes in a book") || s.contains("writes a book")) return new Situation("bookedit", null);
        if (s.contains("edits an armor stand") || s.contains("turns an armor stand") || s.contains("modifies an armor stand")) return new Situation("armorstand", null);
        if (s.contains("starts crafting") || s.contains("begins crafting")) return new Situation("craftstart", null);
        if (s.contains("smiths") || s.contains("smiths an item")) return new Situation("smith", null);
        if (s.contains("a furnace smelts") || s.contains("an item smelts") || s.contains("food cooks in a furnace")) return new Situation("smelt", null);
        if (s.contains("changes his armor") || s.contains("changes armor") || s.contains("puts on armor")
                || s.contains("equips armor") || s.contains("takes off armor")) return new Situation("armorchange", null);
        if (s.contains("gets withered")) return new Situation("wither", null);
        if (s.contains("throws an egg") || s.contains("throws eggs")) return new Situation("eggthrow", null);
        if (s.contains("buckets a fish") || s.contains("captures a fish in a bucket")) return new Situation("bucketcatch", null);
        if (s.contains("burns")) return new Situation("burn", null);

        // --- inventory ---------------------------------------------------------------
        if (s.contains("picks up") || s.contains("pick up")) {
            int i = indexOf(words, "picks") >= 0 ? indexOf(words, "picks") : indexOf(words, "pick");
            return new Situation("pickup", i >= 0 && Math.min(i + 1, words.size() - 1) < words.size()
                    ? join(words.subList(Math.min(i + 2, words.size()), words.size())) : null);
        }
        if (s.contains("switches slot") || s.contains("changes item")) return new Situation("switch", null);
        if (s.contains("opens his inventory") || s.contains("opens her inventory")
                || s.contains("opens inventory") || s.contains("opens a inventory")) return new Situation("invopen", null);

        // --- blocks & crafting --------------------------------------------------------
        if (s.contains("reaches")) {
            int i = indexOf(words, "reaches");
            String p = stripArea(words.subList(i + 1, words.size()));
            return new Situation("reach", p);
        }
        if (s.contains("enters")) {
            int i = indexOf(words, "enters");
            String p = stripArea(words.subList(i + 1, words.size()));
            return new Situation("enter", p);
        }
        if (s.contains("leaves") && !s.contains("leaves the server")) {
            int i = indexOf(words, "leaves");
            String p = stripArea(words.subList(i + 1, words.size()));
            return new Situation("leave", p);
        }
        if (s.contains("collects")) {
            int i = indexOf(words, "collects");
            String p = join(words.subList(i + 1, words.size()));
            return new Situation("collect", p);
        }
        if (s.contains("breaks")) {
            int i = indexOf(words, "breaks");
            String p = join(words.subList(i + 1, words.size()));
            return new Situation("break", p);
        }
        if (s.contains("places a block") || s.contains("places")) {
            int i = indexOf(words, "places");
            String p = join(words.subList(i + 1, words.size()));
            return new Situation("place", p);
        }
        if (s.contains("damages")) {
            int i = indexOf(words, "damages");
            String p = join(words.subList(i + 1, words.size()));
            return new Situation("damage", p);
        }
        if (s.contains("drops") || s.contains("throws")) {
            int i = indexOf(words, "drops") >= 0 ? indexOf(words, "drops") : indexOf(words, "throws");
            String p = join(words.subList(i + 1, words.size()));
            return new Situation("drop", p);
        }
        if (s.contains("crafts")) {
            int i = indexOf(words, "crafts");
            String p = join(words.subList(i + 1, words.size()));
            return new Situation("craft", p);
        }
        if (s.contains("respawns")) {
            return new Situation("respawn", null);
        }
        if (s.contains("eats")) {
            int i = indexOf(words, "eats");
            String p = join(words.subList(i + 1, words.size()));
            return new Situation("eat", p);
        }
        if (s.contains("uses command") || s.contains("uses the command")) {
            int i = indexOf(words, "command");
            String p = join(words.subList(i + 1, words.size()));
            return new Situation("command", p);
        }
        if (s.contains("joins")) return new Situation("join", null);
        if (s.contains("dies")) return new Situation("death", null);

        // add-on event vocabulary
        if (it != null) {
            for (dev.verbum.api.EventWordMapper m : it.nativeEventMappers()) {
                dev.verbum.api.EventWordMapper.EventSpec spec = m.map(words);
                if (spec != null) return new Situation(spec.kind(), spec.param());
            }
        }
        return null;
    }

    // ------------------------------------------------------- ...

    private static String stripArea(List<String> words) {
        if (!words.isEmpty() && words.get(0).equalsIgnoreCase("area")) {
            return join(words.subList(1, words.size()));
        }
        // drop "the"
        if (!words.isEmpty() && words.get(0).equalsIgnoreCase("the")) {
            return join(words.subList(1, words.size()));
        }
        return join(words);
    }

    // ------------------------------------------------------- comparators

    /** Live world values (stats, distance) resolved before the plain compare(). */
    private static Boolean liveCompare(List<String> words, Interpreter it, McRuntime r, String focus, String s) {
        // server has more than 10 players online  |  server has less than 5 players online
        if (s.contains("online") && (s.contains("players") || s.contains(" player "))) {
            double want = -1;
            for (String w : words) if (isNumber(w.replace(",", ""))) { want = Double.parseDouble(w.replace(",", "")); break; }
            if (want < 0) return null;
            int online = r.onlinePlayers();
            if (s.contains("more than") || s.contains("greater than")) return online > want;
            if (s.contains("less than") || s.contains("lower than") || s.contains("fewer than")) return online < want;
            if (s.contains("at least")) return online >= want;
            if (s.contains("at most")) return online <= want;
            return online == want;
        }
        if (s.contains("distance")) {
            int frm = indexOf(words, "from");
            int bet = indexOf(words, "between");
            int fromMark = frm >= 0 ? frm : bet;
            int toMark = indexOf(words, "to");
            if (fromMark >= 0 && toMark > fromMark) {
                String pa = join(words.subList(fromMark + 1, toMark));
                int eqD = indexOf(words, "is") >= 0 ? indexOf(words, "is") : indexOf(words, "equals");
                String pb = eqD >= 0 ? join(words.subList(toMark + 1, eqD)) : join(words.subList(toMark + 1, words.size()));
                double want = 0;
                for (String w : words) if (isNumber(w.replace(",", ""))) { want = Double.parseDouble(w.replace(",", "")); break; }
                double d = r.distance(pa.trim(), pb.trim());
                if (s.contains("more than")) return d > want;
                if (s.contains("less than")) return d < want;
                if (s.contains("at least")) return d >= want;
                if (s.contains("at most")) return d <= want;
                return d == want;
            }
            return Boolean.FALSE;
        }
        boolean statSentence = s.contains("kill streak") || s.contains("kills") || s.contains("deaths")
                || (s.contains("health") && s.contains("percent"))
                || (s.contains("armor") && !s.contains("wearing") && containsNumber(words));
        if (!statSentence) return null;
        int hasWord = indexOf(words, "has");
        int cmpAt = indexOf(words, "is") >= 0 ? indexOf(words, "is")
                : indexOf(words, "are") >= 0 ? indexOf(words, "are")
                : indexOf(words, "equals");
        if (cmpAt < 0 && hasWord < 0) return null;
        double want = -1;
        for (String w : words.subList(Math.max(0, cmpAt >= 0 ? cmpAt : hasWord), words.size())) {
            if (isNumber(w.replace(",", ""))) { want = Double.parseDouble(w.replace(",", "")); break; }
        }
        if (want < 0) return null;
        double live;
        if (s.contains("kill streak")) live = r.killStreak(focus);
        else if (s.contains("deaths")) live = r.deathCount(focus);
        else if (s.contains("kills")) live = r.killCount(focus);
        else if (s.contains("health") && s.contains("percent")) live = r.healthPercent(focus);
        else if (s.contains("armor")) live = r.armor(focus);
        else live = r.killCount(focus);
        if (s.contains("more than")) return live > want;
        if (s.contains("less than")) return live < want;
        if (s.contains("at least")) return live >= want;
        if (s.contains("at most")) return live <= want;
        return live == want;
    }

    private static boolean containsNumber(List<String> words) {
        for (String w : words) if (isNumber(w.replace(",", ""))) return true;
        return false;
    }

    private static final String[] ops = {
            "is at least", "are at least", "is more than", "is greater than",
            "is less than", "is lower than", "is below", "is above",
            "is equal to", "is exactly", "is at most", "no more than",
            "at least", "more than", "greater than", "less than", "lower than",
            "below", "above", "equal to", "equals", "at most"
    };

    private static Boolean compare(List<String> words, Interpreter it) {
        String s = join(words).toLowerCase();
        for (String op : ops) {
            if (s.contains(op)) {
                List<List<String>> parts = splitOn(words, op);
                double right = new MathWords(it).numberOf(parts.get(1), 0);
                double left = resolveLeft(parts.get(0), it);
                return apply(left, right, op);
            }
        }
        return null;
    }

    private static Boolean coordCompare(List<String> words, Interpreter it) {
        String s = join(words).toLowerCase();
        if (s.contains("above y") || s.contains("below y") || s.contains("is at y")
                || s.contains("higher than y") || s.contains("lower than y")) {
            // find y value
            int yIdx = -1;
            for (int i = 0; i < words.size(); i++) if (words.get(i).equalsIgnoreCase("y")) { yIdx = i; break; }
            double val = 0;
            for (int i = words.size() - 1; i >= 0; i--) {
                try { val = Double.parseDouble(words.get(i).replace(",", "")); break; } catch (NumberFormatException ignore) {}
            }
            double current = it.runtime().coord(it.focus(), 'y');
            if (s.contains("above") || s.contains("higher")) return current > val;
            if (s.contains("below") || s.contains("lower")) return current < val;
            return current == val;
        }
        return null;
    }

    private static double resolveLeft(List<String> left, Interpreter it) {
        String l = join(left).toLowerCase();
        // literal number on the left:  5 is greater than 3
        try { return Double.parseDouble(l.replace(",", "")); } catch (NumberFormatException ignore) { }
        // size of a list variable:  length of {quests::*} is at least 3
        if (l.startsWith("length of") || l.startsWith("number of") || l.startsWith("size of")) {
            int of = -1;
            for (int i = 0; i < left.size(); i++) if (left.get(i).equalsIgnoreCase("of")) { of = i; break; }
            Object[] ref = VariableStore.resolve(left.subList(of + 1, left.size()));
            VariableStore.Scope sc = (VariableStore.Scope) ref[0];
            String key = (String) ref[1];
            Object v = it.store().get(sc, key);
            if (v == null) v = dev.verbum.interp.Actions.liveValue(it, sc, key);
            if (v instanceof List<?> ll) return ll.size();
            if (v instanceof Number n) return n.doubleValue();
            return v == null ? 0 : 1;
        }
        String focus = it.focus();
        if (l.contains("health")) return it.runtime().health(focus);
        if (l.contains("level")) return it.runtime().level(focus);
        if (l.contains("food") || l.contains("hunger")) return it.runtime().food(focus);
        // variable
        Object[] ref = VariableStore.resolve(left);
        VariableStore.Scope scope = (VariableStore.Scope) ref[0];
        String key = (String) ref[1];
        if (it.store().has(scope, key)) return it.store().asNumber(it.store().get(scope, key), 0);
        Object live = Actions.liveValue(it, scope, key);
        if (live != null) return it.store().asNumber(live, 0);
        return 0;
    }

    private static boolean apply(double left, double right, String op) {
        switch (op) {
            case "is at least": case "at least": case "are at least": return left >= right;
            case "is more than": case "more than": case "is greater than": case "greater than":
            case "is above": case "above": case "are more than": case "are greater than":
            case "are above": return left > right;
            case "is less than": case "less than": case "is lower than": case "lower than":
            case "is below": case "below": case "are less than": case "are lower than":
            case "are below": return left < right;
            case "is equal to": case "equal to": case "equals": case "is exactly":
            case "are equal to": case "are exactly": return left == right;
            case "no more than": case "is at most": case "at most": case "are at most":
            default: return left <= right;
        }
    }

    // ------------------------------------------------------- split helpers

    private static List<List<String>> split(List<String> words, String marker) {
        List<List<String>> out = new ArrayList<>();
        List<String> cur = new ArrayList<>();
        for (String w : words) {
            if (w.equalsIgnoreCase(marker)) { out.add(cur); cur = new ArrayList<>(); }
            else cur.add(w);
        }
        out.add(cur);
        return out;
    }

    private static List<List<String>> splitOn(List<String> words, String op) {
        String[] opParts = op.split(" ");
        for (int i = 0; i <= words.size() - opParts.length; i++) {
            boolean match = true;
            for (int j = 0; j < opParts.length; j++) {
                if (!words.get(i + j).equalsIgnoreCase(opParts[j])) { match = false; break; }
            }
            if (match) {
                List<String> left = new ArrayList<>(words.subList(0, i));
                List<String> right = new ArrayList<>(words.subList(i + opParts.length, words.size()));
                return List.of(left, right);
            }
        }
        return List.of(new ArrayList<>(words), List.of());
    }

    private static String join(List<String> words) { return VariableStore.join(words); }

    private static int indexOf(List<String> words, String w) {
        for (int i = 0; i < words.size(); i++) if (words.get(i).equalsIgnoreCase(w)) return i;
        return -1;
    }
    private static boolean containsWord(List<String> w, String s) { return indexOf(w, s) >= 0; }
    private static boolean isNumber(String s) { try { Double.parseDouble(s); return true; } catch (NumberFormatException e) { return false; } }
}

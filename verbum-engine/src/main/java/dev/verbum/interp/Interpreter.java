package dev.verbum.interp;

import dev.verbum.ast.*;
import dev.verbum.error.VerbumError;
import dev.verbum.runtime.McRuntime;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Verbum interpreter: it executes parsed programs against a Minecraft
 * runtime, dispatches events, and runs conditions, actions, loops and
 * variables.
 */
public final class Interpreter {

    private final McRuntime runtime;
    private final VariableStore store = new VariableStore();

    private final Map<String, CustomAction> actions = new HashMap<>();
    private final Map<String, CommandHandler> commands = new HashMap<>();
    private final Map<String, MenuBlock> menus = new HashMap<>();
    private final List<EventHandler> everyHandlers = new ArrayList<>();
    private final List<EventHandler> whenHandlers = new ArrayList<>();
    private EventHandler onStart;
    private EventHandler onStop;

    private final Map<String, String> lastOpenedMenu = new HashMap<>();

    private Trigger currentTrigger;
    private final Deque<Map<String, String>> paramStack = new ArrayDeque<>();

    private final Map<EventHandler, Long> everyLastMs = new HashMap<>();
    private long firstTickMs = System.currentTimeMillis();

    // control-flow signals
    private static final class BreakX extends RuntimeException {}
    private static final class ContinueX extends RuntimeException {}
    private static final class StopX extends RuntimeException {}
    /** "return <value>" inside a function; carries the value up to the caller. */
    static final class ReturnX extends RuntimeException {
        final Object value;
        ReturnX(Object value) { this.value = value; }
    }
    /** A cooldown fired: stop the rest of this command silently. */
    static final class OnCooldownX extends RuntimeException {}

    // per-player cooldowns keyed like "cd:<player>:<command>"
    private final Map<String, Long> cooldownUntil = new HashMap<>();

    // wait / delay scheduling
    private static final class WaitSignal extends RuntimeException {
        final long delayMs;
        final String focus;
        WaitSignal(long delayMs, String focus) { this.delayMs = delayMs; this.focus = focus; }
    }
    private record Deferred(List<Stmt> rest, Runnable job, String focus, long dueMs) {}
    private final List<Deferred> deferred = new ArrayList<>();

    // event dispatch state: mirrors Skript's per-event cancellation
    private boolean eventCancelled = false;

    // native addons (see dev.verbum.api)
    private final java.util.Map<String, dev.verbum.api.NativeAction> nativeActions = new HashMap<>();
    private final java.util.Map<String, dev.verbum.api.NativeFunction> nativeFunctions = new HashMap<>();
    private final List<dev.verbum.api.EventWordMapper> nativeEventMappers = new ArrayList<>();
    private final List<dev.verbum.api.NativeCondition> nativeConditions = new ArrayList<>();

    public Interpreter(McRuntime runtime) {
        this.runtime = runtime;
    }

    public McRuntime runtime() { return runtime; }
    public VariableStore store() { return store; }
    public String focus() { return store.focus(); }
    public Trigger currentTrigger() { return currentTrigger; }

    public void load(Program program) {
        for (CustomAction a : program.actions()) actions.put(a.name(), a);
        for (CommandHandler c : program.commands()) commands.put(c.name(), c);
        for (MenuBlock m : program.menus()) menus.put(m.name(), m);
        for (EventHandler h : program.events()) {
            switch (h.kind()) {
                case WHEN -> whenHandlers.add(h);
                case EVERY -> everyHandlers.add(h);
                case ON -> {
                    String t = h.trigger().isEmpty() ? "" : String.join(" ", h.trigger());
                    if (t.equalsIgnoreCase("server start")) onStart = h;
                    else if (t.equalsIgnoreCase("server stop")) onStop = h;
                }
            }
        }
    }

    // ------------------------------------------------------------- public entry points

    public void onServerStart() {
        if (onStart != null) {
            try { runBlock(onStart.body()); } catch (OnCooldownX ignored) { }
        }
    }

    public void onServerStop() {
        if (onStop != null) {
            try { runBlock(onStop.body()); } catch (OnCooldownX ignored) { }
        }
    }

    /** Report a Minecraft event; it may fire matching  when  handlers. */
    public void trigger(String kind, String sourcePlayer) {
        trigger(new Trigger(kind, sourcePlayer));
    }

    public void trigger(Trigger trigger) {
        if (trigger.sourcePlayer != null) store.setFocus(trigger.sourcePlayer);
        currentTrigger = trigger;
        eventCancelled = false;
        try {
            // event-style handlers run in priority order (high -> normal -> low).
            // A  cancel event  action stops every remaining handler for this event.
            List<EventHandler> matched = new ArrayList<>();
            for (EventHandler h : whenHandlers) {
                if (Conditions.eventStyle(h.condition(), this)) matched.add(h);
            }
            matched.sort((a, b) -> Integer.compare(b.priority(), a.priority()));
            for (EventHandler h : matched) {
                if (eventCancelled) break;
                if (Conditions.eval(h.condition(), this, false)) {
                    try {
                        runBlock(h.body());
                    } catch (OnCooldownX ignored) {
                        // cooldown fired inside an event -> silently stop that handler
                    }
                }
            }
        } finally {
            currentTrigger = null;
            eventCancelled = false;
        }
    }

    /** Skript-style  cancel event : no more handlers run for this event. */
    public void cancelEvent() { eventCancelled = true; }
    public boolean isEventCancelled() { return eventCancelled; }

    /**
     * Advance the world clock. Runs "every N seconds" handlers that are due and
     * re-checks condition-style  when  handlers for every online player.
     * Callers should call tick() roughly once a second.
     */
    public void tick() {
        processDeferred();
        runTimers();
        runConditionEvents();
    }

    private void processDeferred() {
        if (deferred.isEmpty()) return;
        long now = System.currentTimeMillis();
        List<Deferred> due = new ArrayList<>();
        deferred.removeIf(d -> { if (d.dueMs() <= now) { due.add(d); return true; } return false; });
        for (Deferred d : due) {
            if (d.job() != null) {
                try { d.job().run(); } catch (StopX | OnCooldownX ignored) { }
                continue;
            }
            store.setFocus(d.focus());
            try {
                runList(d.rest());
            } catch (StopX | OnCooldownX ignored) {
            }
        }
    }

    // ---- custom commands ----------------------------------------------------

    /** Runs a user-defined command, binding its arguments. */
    public void runCommand(String name, List<String> args, String player) {
        CommandHandler c = commands.get(name.toLowerCase());
        if (c == null) {
            throw new VerbumError("I do not know the command  " + name + "\nDefine it with  command " + name);
        }
        if (player != null) store.setFocus(player);
        pushFrame();
        setParam("__verb", c.name());
        List<String> pl = c.parameters();
        for (int i = 0; i < pl.size(); i++) {
            setParam(pl.get(i), i < args.size() ? args.get(i) : "");
        }
        bindArgVars(args);
        try {
            runList(c.body().statements());
        } catch (ReturnX rx) {
            throw new VerbumError(c.line(),
                    "You used  return  outside of a  function.\nTurn  command " + c.name() + "  into  function " + c.name() + "  so it can return a value.");
        } catch (OnCooldownX | StopX ignored) {
            // cooldown fired or  stop  used -> end this command silently
        } finally {
            popFrame();
        }
    }

    /** Lists all user-defined command names (used by the Paper plugin to register them). */
    public java.util.Set<String> commandNames() { return commands.keySet(); }

    public CommandHandler command(String name) { return commands.get(name.toLowerCase()); }

    // ---- cooldowns ----------------------------------------------------------------

    /** The outermost script being run ("command heal", "action reward", ...). */
    public String currentScriptName() {
        String name = "";
        for (Map<String, String> frame : paramStack) {   // innermost frame first
            String v = frame.get("__verb");
            if (v != null) name = v;                     // keep the outer script's name
        }
        return name;
    }

    /** Milliseconds left before the cooldown key cools down (0 = ready). */
    public long cooldownRemaining(String key) {
        Long until = cooldownUntil.get(key);
        if (until == null) return 0;
        long remain = until - System.currentTimeMillis();
        if (remain <= 0) { cooldownUntil.remove(key); return 0; }
        return remain;
    }

    public void putCooldown(String key, long millis) {
        cooldownUntil.put(key, System.currentTimeMillis() + millis);
    }

    /** Throws the signal that stops a command silently while on cooldown. */
    public void stopOnCooldown() { throw new OnCooldownX(); }

    // ---- functions ------------------------------------------------------------

    /** Runs  call name with a and b ...  and returns whatever the function returns. */
    public Object runFunctionCall(List<String> words, int line) {
        // accept both  call name with a   (expression)  and  name with a  (statement args)
        int start = 0;
        if (!words.isEmpty() && words.get(0).equalsIgnoreCase("call")) start = 1;
        if (words.size() < start + 1) {
            throw new VerbumError(line, "I need a function name after  call.\nExample:  call double with 5");
        }
        String name = words.get(start);
        List<String> args = new ArrayList<>();
        int with = indexOfInsensitive(words, start + 1, "with");
        int argStart = with >= 0 ? with + 1 : start + 1;
        // drop a connective  of  after the name:  bigger of 45 and 40 -> bigger 45 and 40
        if (argStart < words.size() && (words.get(argStart).equalsIgnoreCase("of")
                || words.get(argStart).equalsIgnoreCase("between"))) argStart++;
        // split  call name with a and b  into argument groups on "and"
        List<String> cur = new ArrayList<>();
        for (String w : words.subList(argStart, words.size())) {
            if (w.equalsIgnoreCase("and")) {
                if (!cur.isEmpty()) { args.add(evalArg(cur, line)); cur.clear(); }
            } else {
                cur.add(w);
            }
        }
        if (!cur.isEmpty()) args.add(evalArg(cur, line));
        return runFunction(name, args, line);
    }

    /** Whether a custom function (or native add-on function) with this name is defined. */
    public boolean hasFunction(String name) {
        return actions.containsKey(name.toLowerCase()) || nativeFunctions.containsKey(name.toLowerCase());
    }

    /** Runs a function body and returns its  return  value (or null). */
    public Object runFunction(String name, List<String> args, int line) {
        CustomAction fn = actions.get(name.toLowerCase());
        if (fn == null) {
            dev.verbum.api.NativeFunction nativeFn = nativeFunctions.get(name.toLowerCase());
            if (nativeFn != null) return nativeFn.run(this, args, line);
            throw new VerbumError(line,
                    "I do not know the function  " + name + "\nDefine it with  function " + name + "  and call it with  call " + name + " with ...");
        }
        pushFrame();
        setParam("__verb", fn.name());
        List<String> pl = fn.parameters();
        for (int i = 0; i < pl.size(); i++) {
            String value = i < args.size() ? args.get(i) : resolveParamDefault(pl.get(i));
            setParam(pl.get(i), value);
        }
        bindArgVars(args);
        try {
            runBlock(fn.body());
            return null;
        } catch (ReturnX rx) {
            return rx.value;
        } finally {
            popFrame();
        }
    }

    private static int indexOfInsensitive(List<String> words, int from, String target) {
        for (int i = from; i < words.size(); i++) {
            if (words.get(i).equalsIgnoreCase(target)) return i;
        }
        return -1;
    }

    /** Skript-style arguments:  arg-1, argument-1, lastarg, allargs. */
    private void bindArgVars(List<String> args) {
        for (int i = 0; i < args.size(); i++) {
            setParam("arg-" + (i + 1), args.get(i));
            setParam("argument-" + (i + 1), args.get(i));
        }
        setParam("lastarg", args.isEmpty() ? "" : args.get(args.size() - 1));
        setParam("allargs", VariableStore.join(args));
    }

    /**
     * Turns one call argument group into a real value: nested function calls,
     * bare functions and {brace} variables are evaluated, everything else stays
     * as plain text. This is what lets  call double with call double with 5  work.
     */
    private String evalArg(List<String> cur, int line) {
        if (cur.isEmpty()) return "";
        List<String> sub = substitute(cur);
        if (sub.get(0).equalsIgnoreCase("call")) {
            return VariableStore.asText(runFunctionCall(sub, line));
        }
        if (hasFunction(sub.get(0))) {
            List<String> call = new ArrayList<>(sub);
            call.add(0, "call");
            return VariableStore.asText(runFunctionCall(call, line));
        }
        // numeric expressions ("5 plus 1"), {brace} variables and stored values
        // become real values; anything else stays as plain text (a word like
        // "player" is a target, not a number, so never let resolve() object).
        try {
            return VariableStore.asText(Actions.value(this, sub, line));
        } catch (VerbumError e) {
            return VariableStore.join(sub);
        }
    }

    // ---- custom menus (GUIs) ------------------------------------------------

    public MenuBlock menu(String name) { return menus.get(name.toLowerCase()); }

    /** Records that a player opened a menu (the runtime draws the actual GUI). */
    public void openMenu(String menuName, String player) {
        MenuBlock m = menus.get(menuName.toLowerCase());
        if (m == null) {
            throw new VerbumError("I do not know the menu  " + menuName + "\nDefine it with  menu " + menuName);
        }
        lastOpenedMenu.put(player, m.name());
    }

    /** Runs the body of a clicked menu button. */
    public void clickButton(String menuName, String label, String player) {
        MenuBlock m = menus.get(menuName.toLowerCase());
        if (m == null) throw new VerbumError("I do not know the menu  " + menuName);
        for (MenuBlock.Button b : m.buttons()) {
            if (b.label.equalsIgnoreCase(label)) {
                if (player != null) store.setFocus(player);
                try { runBlock(b.body); } catch (OnCooldownX ignored) { }
                return;
            }
        }
        throw new VerbumError("I could not find a button called  " + label + "  in the menu  " + menuName);
    }

    public String lastOpenedMenu(String player) { return lastOpenedMenu.get(player); }


    private void runTimers() {
        if (everyHandlers.isEmpty()) return;
        long now = System.currentTimeMillis();
        for (EventHandler h : everyHandlers) {
            long last = everyLastMs.getOrDefault(h, firstTickMs);
            long intervalMs = h.numberSeconds() == null ? 60000L : h.numberSeconds() * 1000L;
            if (now - last >= intervalMs) {
                everyLastMs.put(h, now);
                try { runBlock(h.body()); }
                catch (VerbumError e) { /* surfaced by caller */ }
                catch (OnCooldownX ignored) { }
            }
        }
    }

    private void runConditionEvents() {
        if (whenHandlers.isEmpty()) return;
        // Evaluate non-event-style handlers once per player. Event-style handlers
        // (touches water, joins...) are matched only inside trigger(...).
        for (String name : onlinePlayerNames()) {
            store.setFocus(name);
            currentTrigger = null;
            eventCancelled = false;
            for (EventHandler h : whenHandlers) {
                if (eventCancelled) break;
                if (!Conditions.eventStyle(h.condition(), this)) {
                    try {
                        if (Conditions.eval(h.condition(), this, false)) {
                            runBlock(h.body());
                        }
                    } catch (StopX | OnCooldownX ignored) { }
                }
            }
        }
        currentTrigger = null;
        eventCancelled = false;
    }

    private List<String> onlinePlayerNames() {
        List<String> out = new ArrayList<>();
        if (runtime instanceof dev.verbum.runtime.MockMcRuntime mock) {
            for (var e : mock.players.entrySet()) if (e.getValue().online) out.add(e.getKey());
        }
        return out;
    }

    // ------------------------------------------------------------- blocks

    public void runBlock(Block block) {
        runList(block.statements());
    }

    /** Executes statements in order, honouring  wait  (which defers the rest). */
    private void runList(List<Stmt> ss) {
        for (int i = 0; i < ss.size(); i++) {
            Stmt s = ss.get(i);
            try {
                exec(s);
            } catch (WaitSignal w) {
                List<Stmt> rest = new ArrayList<>(ss.subList(i + 1, ss.size()));
                String f = w.focus != null ? w.focus : focus();
                if (w.delayMs <= 0) {
                    String prior = focus();
                    store.setFocus(f);
                    try { runList(rest); } finally { store.setFocus(prior); }
                } else {
                    deferred.add(new Deferred(rest, null, f, System.currentTimeMillis() + w.delayMs));
                }
                return;
            } catch (BreakX | ContinueX | StopX e) {
                throw e; // propagate control flow to enclosing loop/handler
            }
        }
    }

    private void exec(Stmt s) {
        if (s instanceof ActionCall call) {
            executeAction(call);
        } else if (s instanceof IfStmt iff) {
            execIf(iff);
        } else if (s instanceof RepeatTimes r) {
            execRepeat(r);
        } else if (s instanceof LoopCondition lc) {
            execLoopCond(lc);
        } else if (s instanceof ForEach fe) {
            execForEach(fe);
        } else if (s instanceof DelayedBlock db) {
            execDelayed(db);
        } else if (s instanceof Return r) {
            throw new ReturnX(Actions.value(this, substitute(r.valueWords()), r.line()));
        } else if (s instanceof Flow f) {
            switch (f.kind()) {
                case BREAK -> throw new BreakX();
                case CONTINUE -> throw new ContinueX();
                case STOP -> throw new StopX();
            }
        }
    }

    private void execIf(IfStmt iff) {
        for (int i = 0; i < iff.conditions().size(); i++) {
            if (Conditions.eval(substitute(iff.conditions().get(i)), this, true)) {
                runBlock(iff.bodies().get(i));
                return;
            }
        }
        runBlock(iff.elseBody());
    }

    private void execRepeat(RepeatTimes r) {
        double n = new MathWords(this).numberOf(substitute(r.countWords()), r.line());
        for (int i = 0; i < n; i++) {
            try { runBlock(r.body()); }
            catch (BreakX e) { break; }
            catch (ContinueX e) { /* continue */ }
        }
    }

    private void execLoopCond(LoopCondition lc) {
        while (true) {
            boolean c = Conditions.eval(substitute(lc.condition()), this, true);
            boolean go = lc.mode() == LoopCondition.Mode.WHILE ? c : !c;
            if (!go) break;
            try { runBlock(lc.body()); }
            catch (BreakX e) { break; }
            catch (ContinueX e) { /* continue */ }
        }
    }

    private void execForEach(ForEach fe) {
        String item = fe.itemName();
        int index = 0;
        for (Object v : new MathWords(this).listOf(substitute(fe.listWords()))) {
            index++;
            store.set(VariableStore.Scope.TEMP, item, v);
            store.set(VariableStore.Scope.TEMP, "loop-value", v);
            store.set(VariableStore.Scope.TEMP, "loop-index", (double) index);
            pushFrame();
            setParam(item, VariableStore.asText(v));
            setParam("loop-value", VariableStore.asText(v));
            setParam("loop-index", String.valueOf(index));
            try { runBlock(fe.body()); }
            catch (BreakX e) { break; }
            catch (ContinueX e) { /* continue */ }
            finally { popFrame(); }
        }
    }

    // ------------------------------------------------------------- actions

    private void executeAction(ActionCall call) {
        String verb = call.verb();
        List<String> args = substitute(call.args());
        if (verb.equalsIgnoreCase("wait")) {
            handleWait(args, call.line());
            return;
        }
        // custom action first, then an add-on native, then the built-in verbs
        CustomAction action = actions.get(verb.toLowerCase());
        if (action != null) {
            runCustomAction(action, args, call.line());
            return;
        }
        dev.verbum.api.NativeAction nativeAction = nativeActions.get(verb.toLowerCase());
        if (nativeAction != null) {
            nativeAction.run(this, args, call.line());
            return;
        }
        Actions.execute(this, verb, args, call.line());
    }

    /** Schedules  after N seconds:  block; the rest of the script continues now. */
    private void execDelayed(DelayedBlock db) {
        List<String> subWords = substitute(db.delayWords());
        double secs = delaySeconds(subWords, db.line());
        String focus = focus();
        List<Stmt> block = db.body().statements();
        deferred.add(new Deferred(null, () -> {
            String prior = store.focus();
            store.setFocus(focus);
            pushFrame();
            try { runList(block); }
            catch (StopX | OnCooldownX ignored) { }
            finally { popFrame(); store.setFocus(prior); }
        }, focus, System.currentTimeMillis() + (long) (secs * 1000)));
    }

    /** Reads a delay like  "5 seconds", "2 minutes"  or a bare number of seconds. */
    private double delaySeconds(List<String> words, int line) {
        for (int i = 0; i < words.size(); i++) {
            try {
                double base = Double.parseDouble(words.get(i).replace(",", ""));
                String unit = i + 1 < words.size() ? words.get(i + 1).toLowerCase() : "seconds";
                if (unit.startsWith("second")) return base;
                if (unit.startsWith("minute")) return base * 60;
                if (unit.startsWith("hour")) return base * 3600;
                if (unit.startsWith("tick")) return base / 20.0;
                return base;
            } catch (NumberFormatException ignore) { }
        }
        return new MathWords(this).numberOf(words, line);
    }

    /** Public scheduler so add-ons can queue work on the verbum event loop. */
    public void runLater(double seconds, Runnable job) {
        deferred.add(new Deferred(null, job, focus(), System.currentTimeMillis() + (long) (seconds * 1000)));
    }

    // ------------------------------------------------------- native add-ons

    public void registerNativeAction(dev.verbum.api.NativeAction a) {
        if (a != null && a.verb() != null) nativeActions.put(a.verb().toLowerCase(), a);
    }
    public void registerNativeFunction(dev.verbum.api.NativeFunction f) {
        if (f != null && f.name() != null) nativeFunctions.put(f.name().toLowerCase(), f);
    }
    public void registerNativeCondition(dev.verbum.api.NativeCondition c) {
        if (c != null) nativeConditions.add(c);
    }
    public void registerEventWordMapper(dev.verbum.api.EventWordMapper m) {
        if (m != null) nativeEventMappers.add(m);
    }
    public dev.verbum.api.NativeFunction nativeFunction(String name) { return nativeFunctions.get(name.toLowerCase()); }
    public List<dev.verbum.api.EventWordMapper> nativeEventMappers() { return nativeEventMappers; }
    public List<dev.verbum.api.NativeCondition> nativeConditions() { return nativeConditions; }

    private void handleWait(List<String> args, int line) {
        for (String a : args) {
            try {
                double secs = Double.parseDouble(a.replace(",", ""));
                throw new WaitSignal((long) (secs * 1000), focus());
            } catch (NumberFormatException ignore) {
            }
        }
        double secs = new MathWords(this).numberOf(args, line);
        throw new WaitSignal((long) (secs * 1000), focus());
    }

    private void runCustomAction(CustomAction action, List<String> args, int line) {
        pushFrame();
        setParam("__verb", action.name());
        List<String> pl = action.parameters();
        for (int i = 0; i < pl.size(); i++) {
            String value = i < args.size() ? args.get(i) : resolveParamDefault(pl.get(i));
            setParam(pl.get(i), value);
        }
        bindArgVars(args);
        try {
            runBlock(action.body());
        } finally {
            popFrame();
        }
    }

    private String resolveParamDefault(String name) {
        return name.equals("player") ? focus() : "";
    }

    private List<String> substitute(List<String> args) {
        List<String> out = new ArrayList<>();
        for (String a : args) out.add(substituteOne(a));
        return out;
    }

    private String substituteOne(String a) {
        String key = a.toLowerCase();
        for (Map<String, String> frame : paramStack) {   // innermost frame first
            if (frame.containsKey(key)) {
                String v = frame.get(key);
                return v == null ? a : v;
            }
        }
        return a;
    }

    public void pushFrame() { paramStack.push(new HashMap<>()); }
    public void popFrame() { paramStack.pop(); }
    public void setParam(String name, String value) {
        if (paramStack.isEmpty()) paramStack.push(new HashMap<>());
        paramStack.peek().put(name.toLowerCase(), value);
    }

    /** Looks up a {brace}-style reference in the param frames: arg-1, lastarg, names. */
    public Object lookupParam(String key) {
        for (Map<String, String> frame : paramStack) {   // innermost frame first
            String v = frame.get(key.toLowerCase());
            if (v != null) return v;
        }
        return null;
    }

    /** Friendly error entry point used by sub-engines. */
    public static VerbumError err(int line, String msg) { return new VerbumError(line, msg); }
}

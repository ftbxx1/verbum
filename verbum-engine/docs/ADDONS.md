# Addons & the Verbum Ecosystem

Verbum ships with a large built-in verb library, but the *ecosystem* is what
makes it extensible: **any plugin can teach Verbum new verbs, functions,
conditions and event words** without changing Verbum's source, and scripts can
then use them in plain English.

This is how an ExcellentCrates-style plugin integrates.

## How a plugin joins (two ways)

### 1. As a Bukkit add-on (recommended)

Make your plugin depend on Verbum in `plugin.yml`:

```yml
name: ExcellentCrates
main: io.example.crates.CratesPlugin
depend: [Verbum]
```

In your `onEnable`, grab the API and register a `VerbumPlugin`:

```java
import dev.verbum.api.VerbumAPI;
import dev.verbum.api.VerbumPlugin;
import dev.verbum.api.NativeAction;
import dev.verbum.api.PluginBridge;
import dev.verbum.interp.Interpreter;
import java.util.List;
```

The simplest form:

```java
@Override
public void onEnable() {
    VerbumAPI api = VerbumAPI.get();
    if (!api.isAvailable()) return;          // Verbum not loaded yet
    api.registerPlugin(new CratesAddon());   // Verbum registers it for you
}
```

Because your plugin declared `depend: [Verbum]`, Verbum is already enabled and
has published its engine, so `isAvailable()` is `true` right away. If your
plugin must load before Verbum for some reason, just hold the call in a delayed
task and check `isAvailable()` again — Verbum keeps your plugin instance and
re-registers it on every `/verbum reload`.

### Registration methods on the API

`EngineRegistrar` (which `ScriptEngine` is, and which you get through
`VerbumAPI.get().engine()`) exposes:

- `registerAction(NativeAction)` — a new verb (single- or multi-word)
- `registerFunction(NativeFunction)` — `call crate count with ...` / expressions
- `registerCondition(NativeCondition)` — a live yes/no check
- `registerEvent(EventWordMapper)` — new `when ...` event words
- `registerPlugin(VerbumPlugin)` — a full plugin (name + bridge + vocab)

`NativeAction.of("create crate", (it, words, line) -> { ... })` is the shortcut.

## Two integration styles for your verbs

**Style A — your own multi-word verbs (preferred).** Register a verb that
consumes more than one word. Verbum tries the longest matching prefix first, so
your `create crate` beats the built-in `create` when the script says
`create crate KeyCrate`:

```java
reg.registerAction(NativeAction.of("create crate",
    (it, words, line) -> {
        String name = words.isEmpty() ? "KeyCrate" : words.get(0);
        it.runtime().announce("crate created " + name);
    }));
```

Script:

```
when the player joins
    create crate KeyCrate
```

**Style B — a single `plugin <name> ...` dispatch.** Implement `PluginBridge` so
scripts call one named keyword with their own phrase grammar. This is the
"command surface" your plugin owns entirely:

```java
public class CratesAddon implements VerbumPlugin, PluginBridge {
    @Override public String name() { return "ExcellentCrates"; }
    @Override public PluginBridge bridge() { return this; }

    @Override public void action(Interpreter it, List<String> words, int line) {
        if (!words.isEmpty() && words.get(0).equalsIgnoreCase("open")) {
            it.runtime().announce(it.focus() + " opened a crate");
        }
    }
    @Override public boolean condition(Interpreter it, List<String> words) {
        return words.size() > 1 && words.get(0).equalsIgnoreCase("has")
            && keysGivenTo(it.focus());
    }
    @Override public void register(EngineRegistrar r) { /* your NativeActions */ }
}
```

Script:

```
when the player joins
    plugin ExcellentCrates give key
    if plugin ExcellentCrates has key
        tell player You can open
```

## /verbum reload

When a server runs `/verbum reload`, Verbum builds a fresh engine. Your plugin
instance stays registered, so Verbum asks it to re-register against the new
engine automatically — your verbs keep working across reloads.

## Live conditions & variables

Everything you register can also be **asked about** in `if`/`when`:

- `NativeCondition` is matched via `matches(words)` and returns a boolean.
- A plain `NativeAction` that is only ever used in questions: you can also just
  answer with a live variable. Built-in actions already answer things like
  `player is jailed`, `player has a home`, `player is riding`, `player is
  whitelisted`, `player's ping`, `player's tps`, `world border`, etc.

## Example

A minimal reference add-on lives in `verbum-engine/src/test/java/dev/verbum/`
as `EcosystemAddonTest` — read it as a worked example of a crate add-on with a
multi-word verb, a live condition, a function and a `plugin` bridge, plus the
reload-survives test.

## Writing tests for your add-on

You do not need a live server. Use the mock:

```java
ScriptEngine engine = new ScriptEngine(new MockMcRuntime());
VerbumAPI.publish(engine);
engine.registerPlugin(new MyAddon());
engine.load("...your .vb...", "t.vb");
engine.trigger("join", "Steve");
engine.tick();
assertTrue(runtime.chatter.stream().anyMatch(l -> l.contains("...")));
```

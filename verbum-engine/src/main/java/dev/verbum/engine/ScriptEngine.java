package dev.verbum.engine;

import dev.verbum.api.EngineRegistrar;
import dev.verbum.api.VerbumPlugin;
import dev.verbum.ast.Program;
import dev.verbum.i18n.KeywordResolver;
import dev.verbum.interp.Interpreter;
import dev.verbum.interp.Trigger;
import dev.verbum.lex.Line;
import dev.verbum.lex.Tokenizer;
import dev.verbum.parser.Parser;
import dev.verbum.runtime.McRuntime;
import dev.verbum.store.DataStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The top-level facade. It reads a .vb file, tokenizes, parses, and hands the
 * program to the interpreter, which talks to the Minecraft runtime.
 *
 * It is also the extension point of the ecosystem: add-ons implement
 * {@link dev.verbum.api.VerbumPlugin} and register through
 * {@link #registerPlugin(VerbumPlugin)}.
 */
public final class ScriptEngine implements EngineRegistrar {

    private final McRuntime runtime;
    private final Interpreter interpreter;
    private final List<String> pluginNames = new ArrayList<>();
    private DataStore dataStore;
    private KeywordResolver resolver = KeywordResolver.ENGLISH;

    public ScriptEngine(McRuntime runtime) {
        this.runtime = runtime;
        this.interpreter = new Interpreter(runtime);
    }

    /** Sets the keyword language for future loads ("es", "de", "fr", ...). Unknown codes fall back to English. */
    public ScriptEngine setLanguage(String code) {
        this.resolver = KeywordResolver.forCode(code);
        return this;
    }

    /** Sets the keyword language by its native name ("spanish", "español", "deutsch", ...). */
    public ScriptEngine setLanguageByName(String name) {
        this.resolver = KeywordResolver.forName(name);
        return this;
    }

    /** The language this engine is currently writing keywords in. */
    public KeywordResolver resolver() { return resolver; }

    public Interpreter interpreter() { return interpreter; }
    public McRuntime runtime() { return runtime; }

    /** Load source text with a friendly filename used in errors. */
    public void load(String source, String filename) {
        List<Line> lines = new Tokenizer(source, filename).tokenize();
        Program program = new Parser(lines, resolver).parse();
        interpreter.load(program);
    }

    /** Load and parse a script file from disk. */
    public void loadFile(String path) {
        try {
            String source = Files.readString(Path.of(path));
            load(source, path);
        } catch (IOException e) {
            throw new dev.verbum.error.VerbumError("I could not read the file: " + path + "\n" + e.getMessage());
        }
    }

    // ---- convenience hooks -------------------------------------------------

    public void onServerStart() { interpreter.onServerStart(); }
    public void onServerStop() { interpreter.onServerStop(); }

    public void trigger(String kind, String player) { interpreter.trigger(kind, player); }

    public void trigger(Trigger t) { interpreter.trigger(t); }

    /** Advance simulation time; runs every/condition handlers. Call roughly each second. */
    public void tick() { interpreter.tick(); }

    // ---- ecosystem: add-ons ------------------------------------------------

    @Override public void registerAction(dev.verbum.api.NativeAction a) { interpreter.registerNativeAction(a); }
    @Override public void registerFunction(dev.verbum.api.NativeFunction f) { interpreter.registerNativeFunction(f); }
    @Override public void registerCondition(dev.verbum.api.NativeCondition c) { interpreter.registerNativeCondition(c); }
    @Override public void registerEvent(dev.verbum.api.EventWordMapper m) { interpreter.registerEventWordMapper(m); }

    @Override public void registerPlugin(VerbumPlugin plugin) {
        if (plugin == null) return;
        String name = plugin.name() == null ? "anonymous" : plugin.name();
        if (!pluginNames.contains(name)) pluginNames.add(name);
        plugin.register(this);
    }

    @Override public List<String> plugins() { return new ArrayList<>(pluginNames); }

    // ---- persistence --------------------------------------------------------

    /** Attach a store (e.g. a {@link dev.verbum.store.JsonDataStore} file). */
    public void setDataStore(DataStore store) { this.dataStore = store; }

    /** Load saved variables from the attached store, if any. */
    public void loadVariables() {
        if (dataStore != null) interpreter.store().restore(dataStore.load());
    }

    /** Write all durable variables to the attached store, if any. */
    public void saveVariables() {
        if (dataStore != null) dataStore.save(interpreter.store().snapshot());
    }

    /** One-shot load:  ScriptEngine.loadVariables(Path)  without wiring a store. */
    public void loadVariables(Path file) {
        setDataStore(new dev.verbum.store.JsonDataStore(file));
        loadVariables();
    }

    /** One-shot save:  ScriptEngine.saveVariables(Path). */
    public void saveVariables(Path file) {
        setDataStore(new dev.verbum.store.JsonDataStore(file));
        saveVariables();
    }

    public Map<String, Object> snapshotVariables() { return interpreter.store().snapshot(); }
    public void restoreVariables(Map<String, Object> data) { interpreter.store().restore(data); }
}
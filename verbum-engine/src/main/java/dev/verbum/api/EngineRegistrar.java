package dev.verbum.api;

import java.util.List;

/**
 * What an add-on can add to the engine: new action verbs, functions,
 * conditions, and event vocabulary. Plugins receive this in
 * {@link VerbumPlugin#register(EngineRegistrar)}.
 */
public interface EngineRegistrar {

    void registerAction(NativeAction action);
    void registerFunction(NativeFunction function);
    void registerCondition(NativeCondition condition);
    void registerEvent(EventWordMapper mapper);

    /** Registers a whole plugin (name + registrar callback). */
    void registerPlugin(VerbumPlugin plugin);

    /** List of registered plugin names (for introspection / CLI). */
    List<String> plugins();
}
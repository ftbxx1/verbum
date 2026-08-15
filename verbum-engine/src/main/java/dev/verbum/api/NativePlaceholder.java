package dev.verbum.api;

import dev.verbum.interp.Interpreter;
import java.util.List;

/**
 * A native placeholder usable as <pre>%placeholder%</pre> in script text
 * (e.g. send "%player_name%" to player).
 *
 * The addon provides a live-value supplier that resolves at runtime.
 *
 * <pre>
 *   r.registerPlaceholder(NativePlaceholder.of("player_name",
 *       it -> it.focus()));
 * </pre>
 */
public interface NativePlaceholder {

    /** The placeholder name (without the % signs). */
    String name();

    /** Resolve the placeholder value for the current focus. */
    String resolve(Interpreter interp);

    /** Shortcut factory:  placeholder("player_name", it -> ...) */
    static NativePlaceholder of(String name, Resolver res) {
        return new NativePlaceholder() {
            @Override public String name() { return name; }
            @Override public String resolve(Interpreter interp) { return res.resolve(interp); }
        };
    }

    @FunctionalInterface
    interface Resolver { String resolve(Interpreter interp); }
}
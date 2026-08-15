package dev.verbum.api;

import dev.verbum.interp.Interpreter;

/**
 * A native property accessible as <pre>{player's health}</pre> or
 * <pre>{world's time}</pre> from scripts.
 *
 * The addon provides a live-value supplier. For example:
 *   r.registerProperty(NativeProperty.of("health",
 *       it -> it.runtime().health(it.focus())));
 *
 * Script: send "%player's health%" to player
 */
public interface NativeProperty {

    /** The property name (without "player's" or "world's" prefix). */
    String name();

    /** Evaluate the property for the current focus. */
    Object value(Interpreter interp);

    /** Shortcut factory:  property("health", it -> ...) */
    static NativeProperty of(String name, Supplier sup) {
        return new NativeProperty() {
            @Override public String name() { return name; }
            @Override public Object value(Interpreter interp) { return sup.get(interp); }
        };
    }

    @FunctionalInterface
    interface Supplier { Object get(Interpreter interp); }
}
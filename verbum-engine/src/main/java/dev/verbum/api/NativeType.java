package dev.verbum.api;

import java.util.List;

/**
 * A native type registered by an add-on, usable in type declarations and
 * coercion. For example, a plugin might register a "player health bar" type
 * or a "coordinate" type that Verbum understands beyond plain numbers.
 *
 * The addon provides:
 *   - matches(words)       → whether this phrase is this type
 *   - coerce(v, line)      → convert a Java value to this type
 *   - label()              → human label for error messages
 *
 * <pre>
 *   r.registerType(NativeType.of("health bar",
 *       words -> join(words).startsWith("health bar"),
 *       (v, line) -> ...,
 *       "a health bar value"));
 * </pre>
 */
public interface NativeType {

    /** The human label used in type declarations and errors. */
    String label();

    /** Whether these words belong to this type (checked before built-in types). */
    boolean matches(List<String> words);

    /** Coerce a Java value into this type, or throw a friendly error. */
    Object coerce(Object v, int line);

    /** Factory:  type("health bar", ...) */
    static NativeType of(String lbl, Caller caller, Coercer coercer) {
        return new NativeType() {
            @Override public String label() { return lbl; }
            @Override public boolean matches(List<String> words) { return caller.run(words); }
            @Override public Object coerce(Object v, int line) { return coercer.run(v, line); }
        };
    }

    /** Called to decide if words match this type. */
    @FunctionalInterface
    interface Caller { boolean run(List<String> words); }

    /** Called to coerce a value into this type. */
    @FunctionalInterface
    interface Coercer { Object run(Object v, int line); }
}
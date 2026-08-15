package dev.verbum.ast;

import dev.verbum.type.Type;

/**
 * One parameter of a user-defined function.
 *
 * <p>Syntax in scripts:
 * <pre>
 *   function reward(player, kills as number = 1) returns number
 * </pre>
 * "player" is positional/unnamed (no type, no default), "kills" is typed and
 * defaulted. A parameter that is not passed at the call site uses its default.
 */
public final class Parameter {

    private final String name;
    private final Type type;
    private final Object defaultValue;   // null means "no default -> required"
    private final boolean optional;      // true when a default was given

    public Parameter(String name) {
        this(name, Type.ANY, null, false);
    }

    public Parameter(String name, Type type, Object defaultValue, boolean optional) {
        this.name = name.toLowerCase();
        this.type = type == null ? Type.ANY : type;
        this.defaultValue = defaultValue;
        this.optional = optional;
    }

    public String name() { return name; }
    public Type type() { return type; }
    public Object defaultValue() { return defaultValue; }
    public boolean isOptional() { return optional; }

    /** Whether the call site supplied this many args would satisfy this param. */
    public boolean accepts(int argCount, int paramIndex) {
        return argCount > paramIndex || optional;
    }
}

package dev.verbum.type;

import dev.verbum.error.VerbumError;

/**
 * The set of types a Verbum value can have. The runtime still carries values as
 * plain Java objects (Double, String, Boolean, List, ...), but every value also
 * carries a Type tag so functions can declare what they accept and return, and
 * so error messages can say exactly what went wrong instead of "got text".
 *
 * Unknown names fall back to {@link #ANY}.
 */
public enum Type {
    ANY("anything", Object.class),
    NUMBER("number", Number.class),
    INTEGER("integer", Number.class),
    DECIMAL("decimal", Number.class),
    TEXT("text", String.class),
    BOOLEAN("boolean", Boolean.class),
    LIST("list", java.util.List.class),
    MAP("map", java.util.Map.class),
    PLAYER("player", String.class),
    OFFLINE_PLAYER("offline player", String.class),
    ENTITY("entity", String.class),
    LIVING_ENTITY("living entity", String.class),
    ITEM("item", String.class),
    BLOCK("block", String.class),
    MATERIAL("material", String.class),
    LOCATION("location", Object.class),
    VECTOR("vector", Object.class),
    WORLD("world", String.class),
    INVENTORY("inventory", Object.class),
    SLOT("slot", Object.class),
    SOUND("sound", String.class),
    PARTICLE("particle", String.class),
    POTION_EFFECT("potion effect", String.class),
    ENCHANTMENT("enchantment", String.class),
    DURATION("duration", Number.class),
    DATETIME("date/time", Object.class),
    UUID("uuid", String.class);

    private final String label;
    private final Class<?> box;

    Type(String label, Class<?> box) { this.label = label; this.box = box; }

    /** Human label used in error messages:  "number", "an item", "text". */
    public String label() { return label; }

    /** Parses a type word from a script, e.g. "number" -> NUMBER, else ANY. */
    public static Type of(String name) {
        if (name == null) return ANY;
        String n = name.toLowerCase().replace(" ", "");
        for (Type t : values()) if (t.name().toLowerCase().equals(n)) return t;
        for (Type t : values()) if (t.label.replace(" ", "").equalsIgnoreCase(n)) return t;
        return ANY;
    }

    /** Coerces a Java value into something this type accepts, or throws a type error. */
    public Object coerce(Object v, int line) {
        if (v == null) {
            if (this == BOOLEAN) return Boolean.FALSE;
            if (this == NUMBER || this == INTEGER || this == DECIMAL || this == DURATION) return 0.0;
            if (this == TEXT || this == PLAYER || this == OFFLINE_PLAYER) return "";
            if (this == LIST) return java.util.List.of();
            if (this == MAP) return java.util.Map.of();
            return null;
        }
        if (this == ANY) return v;
        // numeric types always normalize to Double so the engine has one number type
        if (this == NUMBER || this == INTEGER || this == DECIMAL || this == DURATION) {
            if (v instanceof Number n) return n.doubleValue();
            if (v instanceof String s) {
                try { return Double.parseDouble(s.replace(",", "")); } catch (NumberFormatException ignore) {}
            }
            if (v instanceof Boolean b) return b ? 1.0 : 0.0;
            throw new VerbumError(line,
                    "I cannot use " + format(v) + " as " + this.label + ".\n" +
                    "Verbum expected a " + this.label + " here.");
        }
        if (box.isInstance(v)) return v;
        try {
            if (this == TEXT || this == PLAYER || this == OFFLINE_PLAYER
                    || this == MATERIAL || this == SOUND || this == PARTICLE) {
                return String.valueOf(v);
            }
            if (this == BOOLEAN) {
                if (v instanceof String s) return s.equalsIgnoreCase("true") || s.equalsIgnoreCase("yes");
                if (v instanceof Number n) return n.doubleValue() != 0;
            }
            if (this == LIST) {
                if (v instanceof java.util.List) return v;
                return java.util.List.of(v);
            }
            if (this == MAP) {
                if (v instanceof java.util.Map) return v;
            }
        } catch (Exception e) {
            throw new VerbumError(line,
                    "I cannot use " + format(v) + " as " + this.label + ".\n" +
                    "Verbum expected a " + this.label + " here.");
        }
        throw new VerbumError(line,
                "I tried to use " + format(v) + ", but a " + this.label + " was expected.\n" +
                "For example, give " + (this == ITEM ? "an item" : this.label) + " instead.");
    }

    /** Type-check a value (no coercion); returns the value or throws a friendly error. */
    public Object check(Object v, int line) {
        if (v == null) return v;
        if (this == ANY) return v;
        if (box.isInstance(v)) return v;
        throw new VerbumError(line,
                "I tried to use " + format(v) + ", but a " + this.label + " was expected.\n" +
                "For example, give " + (this == ITEM ? "an item" : this.label) + " instead.");
    }

    private static String format(Object v) {
        String s = (v instanceof String str) ? "\"" + str + "\"" : String.valueOf(v);
        return s.length() > 40 ? s.substring(0, 40) + "..." : s;
    }
}

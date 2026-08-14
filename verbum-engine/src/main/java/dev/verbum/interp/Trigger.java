package dev.verbum.interp;

import java.util.HashMap;
import java.util.Map;

/**
 * A single Minecraft event that a runtime can report, e.g. "touches water" or
 * "breaks diamond ore". The kind is a canonical situation and params carry the
 * details (which block, which area, which amount).
 */
public final class Trigger {

    public final String kind;
    public final Map<String, String> params = new HashMap<>();
    public final String sourcePlayer;

    public Trigger(String kind, String sourcePlayer) {
        this.kind = kind;
        this.sourcePlayer = sourcePlayer;
    }

    public Trigger with(String key, String value) {
        params.put(key, value);
        return this;
    }

    public String param(String key) { return params.get(key); }
}

package dev.verbum.api;

import dev.verbum.interp.Interpreter;

import java.util.List;

/**
 * Lets an add-on extend the event vocabulary. For example a jail add-on can
 * map  when player escapes jail  to the trigger kind "escape" so scripts and
 * the runtime agree on one canonical event.
 */
@FunctionalInterface
public interface EventWordMapper {

    /** Returns {@code null} when the words are not this add-on's event. */
    EventSpec map(List<String> words);

    /** The canonical event this mapper understands. */
    record EventSpec(String kind, String param) {
        public EventSpec(String kind) { this(kind, null); }
    }

    /** Helper so mapping ignores cases: wraps a mapper with a fixed event name. */
    static EventWordMapper fixed(String kind, String... triggerWords) {
        return words -> {
            String joined = String.join(" ", words).toLowerCase();
            for (String w : triggerWords) {
                if (joined.contains(w.toLowerCase())) return new EventSpec(kind);
            }
            return null;
        };
    }
}
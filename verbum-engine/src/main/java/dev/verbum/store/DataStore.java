package dev.verbum.store;

import java.util.Map;

/**
 * Where saved Verbum variables go. A runtime can supply its own (SQLite, YAML,
 * a Bukkit config file) or use the bundled {@link JsonDataStore} which writes a
 * simple JSON file.
 */
public interface DataStore {

    Map<String, Object> load();

    void save(Map<String, Object> variables);
}
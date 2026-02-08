package com.evolution.dropfile.store.store;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Optional;

public interface KeyValueStore<V> {

    V save(String key, V value);

    V remove(String key);

    void removeAll();

    Map<String, V> getAll();

    default void validate(String key, V value) {

    }

    default Optional<Map.Entry<String, V>> get(String key) {
        return Optional.ofNullable(getAll().get(key))
                .map(it -> new AbstractMap.SimpleEntry<>(key, it));
    }

    default Map.Entry<String, V> getRequired(String key) {
        return get(key)
                .orElseThrow(() -> new RuntimeException(String.format(
                    "Store %s. No key %s found", getClass().getName(), key
                )));
    }
}

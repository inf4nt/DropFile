package com.evolution.dropfile.store.store;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Optional;

public interface KeyValueStore<K, V> {

    V save(K key, V value);

    V remove(K key);

    void removeAll();

    Map<K, V> getAll();

    default void validate(K key, V value) {

    }

    default Optional<Map.Entry<K, V>> get(K key) {
        return Optional.ofNullable(getAll().get(key))
                .map(it -> new AbstractMap.SimpleEntry<>(key, it));
    }

    default Map.Entry<K, V> getRequired(K key) {
        return get(key)
                .orElseThrow(() -> new RuntimeException(String.format(
                    "Store %s. No key %s found", getClass().getName(), key
                )));
    }
}

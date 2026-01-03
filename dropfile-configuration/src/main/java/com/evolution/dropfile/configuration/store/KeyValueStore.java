package com.evolution.dropfile.configuration.store;

import java.util.Map;
import java.util.Optional;

public interface KeyValueStore<K, V> {

    V save(K key, V value);

    V remove(K key);

    void removeAll();

    Map<K, V> getAll();

    default Optional<V> get(K key) {
        return Optional.ofNullable(getAll().get(key));
    }

    default V getRequired(K key) {
        return get(key).orElseThrow();
    }
}

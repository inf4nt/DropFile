package com.evolution.dropfile.configuration.store;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class RuntimeKeyValueStore<K, V> implements KeyValueStore<K, V> {

    private final Map<K, V> store = Collections.synchronizedMap(new LinkedHashMap<>());

    @Override
    public V save(K key, V value) {
        return store.compute(key, (k, v) -> {
            validate(key, value);
            return value;
        });
    }

    @Override
    public V remove(K key) {
        return store.remove(key);
    }

    @Override
    public void removeAll() {
        store.clear();
    }

    @Override
    public Map<K, V> getAll() {
        return Map.copyOf(store);
    }
}

package com.evolution.dropfile.configuration.store;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RuntimeKeyValueStore<K, V> implements KeyValueStore<K, V> {

    private final Map<K, V> store = new ConcurrentHashMap<>();

    @Override
    public V save(K key, V value) {
        return store.compute(key, (k, v) -> value);
    }

    @Override
    public V remove(K key) {
        return store.remove(key);
    }

    @Override
    public Map<K, V> getAll() {
        return Map.copyOf(store);
    }
}

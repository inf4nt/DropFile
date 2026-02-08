package com.evolution.dropfile.store.store;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class RuntimeKeyValueStore<V> implements KeyValueStore<V> {

    private final Map<String, V> store = Collections.synchronizedMap(new LinkedHashMap<>());

    @Override
    public V save(String key, V value) {
        return store.compute(key, (k, v) -> {
            validate(key, value);
            return value;
        });
    }

    @Override
    public V remove(String key) {
        return store.remove(key);
    }

    @Override
    public void removeAll() {
        store.clear();
    }

    @Override
    public Map<String, V> getAll() {
        return Map.copyOf(store);
    }
}

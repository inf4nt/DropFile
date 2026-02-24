package com.evolution.dropfile.store.store;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

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
    public V update(String key, Function<V, V> updateFunction) {
        return store.compute(key, (k, v) -> {
            if (v == null) {
                throw new RuntimeException(String.format(
                        "Store %s. No key %s found", getClass().getName(), key
                ));
            }
            V newValue = updateFunction.apply(v);
            validate(key, newValue);
            return newValue;
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

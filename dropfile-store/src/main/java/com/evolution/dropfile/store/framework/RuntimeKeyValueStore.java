package com.evolution.dropfile.store.framework;

import java.util.*;
import java.util.function.Function;

public class RuntimeKeyValueStore<V> implements KeyValueStore<V> {

    private final Map<String, V> store = new LinkedHashMap<>();

    @Override
    public synchronized Collection<V> save(Map<String, V> values) {
        if (values.isEmpty()) {
            return Collections.emptyList();
        }

        for (Map.Entry<String, V> entry : values.entrySet()) {
            Objects.requireNonNull(entry.getKey(), "key cannot be null");
            Objects.requireNonNull(entry.getValue(), "value cannot be null");
        }

        for (Map.Entry<String, V> entry : values.entrySet()) {
            validate(entry.getKey(), entry.getValue());
        }

        Map<String, V> newMap = new LinkedHashMap<>(getAll());
        newMap.putAll(values);
        store.putAll(newMap);
        return Collections.unmodifiableCollection(values.values());
    }

    @Override
    public synchronized V update(String key, Function<V, V> updateFunction) {
        V current = getRequired(key).getValue();
        V newValue = updateFunction.apply(current);
        validate(key, newValue);
        store.put(key, newValue);
        return newValue;
    }

    @Override
    public synchronized V remove(String key) {
        return store.remove(key);
    }

    @Override
    public synchronized void removeAll() {
        store.clear();
    }

    @Override
    public synchronized Map<String, V> getAll() {
        return Map.copyOf(store);
    }
}

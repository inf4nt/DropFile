package com.evolution.dropfile.store.framework;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class RuntimeKeyValueStore<V> implements KeyValueStore<V> {

    private final Map<String, V> store = new LinkedHashMap<>();

    @Override
    public synchronized Collection<V> save(Supplier<? extends Map<String, V>> supplier, ValidatePolicy validatePolicy) {
        Objects.requireNonNull(supplier);
        Objects.requireNonNull(validatePolicy);
        Map<String, V> newValues = supplier.get();
        if (newValues == null || newValues.isEmpty()) {
            return Collections.emptyList();
        }

        for (Map.Entry<String, V> entry : newValues.entrySet()) {
            Objects.requireNonNull(entry.getKey(), "key cannot be null");
            Objects.requireNonNull(entry.getValue(), "value cannot be null");
        }

        Map<String, V> toSave = new LinkedHashMap<>();
        for (Map.Entry<String, V> entry : newValues.entrySet()) {
            try {
                validate(entry.getKey(), entry.getValue());
                toSave.put(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                if (validatePolicy == ValidatePolicy.GENTLE) {
                    continue;
                } else if (validatePolicy == ValidatePolicy.STRICT) {
                    throw e;
                }
                throw new IllegalArgumentException("Unknown validate policy " + validatePolicy);
            }
        }

        if (toSave.isEmpty()) {
            return Collections.emptyList();
        }

        store.putAll(toSave);

        return Collections.unmodifiableCollection(toSave.values());
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

    @Override
    public void init() {

    }
}

package com.evolution.dropfile.store.framework.single;

import com.evolution.dropfile.store.framework.KeyValueStore;

import java.util.Map;
import java.util.Optional;

public class DefaultSingleValueStore<V> implements SingleValueStore<V> {

    private static final String VALUE_NAME = "_";

    protected final KeyValueStore<V> store;

    public DefaultSingleValueStore(KeyValueStore<V> store) {
        this.store = store;
    }

    @Override
    public Optional<V> get() {
        return store.get(VALUE_NAME).map(Map.Entry::getValue);
    }

    @Override
    public V save(V value) {
        return store.save(VALUE_NAME, () -> {
            validate(value);
            return value;
        });
    }

    @Override
    public V remove() {
        return store.remove(VALUE_NAME);
    }
}

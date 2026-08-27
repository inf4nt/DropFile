package com.evolution.dropfile.store.framework.bootstrap;

import com.evolution.dropfile.store.framework.KeyValueStore;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
public class DefaultBootstrapStore<V> implements BootstrapStore<V> {

    private static final String VALUE_NAME = "_";

    protected final KeyValueStore<V> store;

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

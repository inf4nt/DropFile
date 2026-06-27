package com.evolution.dropfile.store.framework.bootstrap;

import com.evolution.dropfile.store.framework.KeyValueStore;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
public class DefaultBootstrapStore<V> implements BootstrapStore<V> {

    private final String storeName;

    protected final KeyValueStore<V> store;

    @Override
    public Optional<V> get() {
        return store.get(storeName).map(Map.Entry::getValue);
    }

    @Override
    public V save(V value) {
        return store.save(storeName, () -> {
            validate(value);
            return value;
        });
    }

    @Override
    public V remove() {
        return store.remove(storeName);
    }
}

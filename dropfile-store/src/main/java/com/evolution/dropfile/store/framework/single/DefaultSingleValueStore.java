package com.evolution.dropfile.store.framework.single;

import com.evolution.dropfile.store.framework.KeyValueStore;

import java.util.Map;
import java.util.Optional;

public class DefaultSingleValueStore<V> implements SingleValueStore<V> {

    private final String storeName;

    private final KeyValueStore<V> store;

    public DefaultSingleValueStore(String storeName,
                                   KeyValueStore<V> store) {
        this.storeName = storeName;
        this.store = store;
    }

    @Override
    public void init() {
        store.init();
    }

    @Override
    public Optional<V> get() {
        return store.get(storeName).map(Map.Entry::getValue);
    }

    @Override
    public V save(V value) {
        validate(value);
        return store.save(storeName, value);
    }

    @Override
    public V remove() {
        return store.remove(storeName);
    }
}

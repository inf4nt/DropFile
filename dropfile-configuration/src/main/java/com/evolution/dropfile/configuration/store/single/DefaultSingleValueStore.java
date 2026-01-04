package com.evolution.dropfile.configuration.store.single;

import com.evolution.dropfile.configuration.store.KeyValueStore;

import java.util.Map;
import java.util.Optional;

public class DefaultSingleValueStore<V> implements SingleValueStore<V> {

    private final String storeName;

    private final KeyValueStore<String, V> store;

    public DefaultSingleValueStore(String storeName,
                                   KeyValueStore<String, V> store) {
        this.storeName = storeName;
        this.store = store;
    }

    @Override
    public Optional<V> get() {
        return store.get(storeName).map(Map.Entry::getValue);
    }

    @Override
    public V save(V value) {
        return store.save(storeName, value);
    }

    @Override
    public V remove() {
        return store.remove(storeName);
    }
}

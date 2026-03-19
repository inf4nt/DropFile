package com.evolution.dropfile.store.framework.single;

import com.evolution.dropfile.store.framework.CacheableKeyValueStore;

import java.util.Map;
import java.util.Optional;

public class CacheableDefaultSingleValueStore<V>
        implements CacheableSingleValueStore<V> {

    private final String storeName;

    protected final CacheableKeyValueStore<V> store;

    public CacheableDefaultSingleValueStore(String storeName,
                                            CacheableKeyValueStore<V> store) {
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
        return store.save(storeName, () -> {
            validate(value);
            return value;
        });
    }

    @Override
    public V remove() {
        return store.remove(storeName);
    }

    @Override
    public void reset() {
        store.reset();
    }
}

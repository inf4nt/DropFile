package com.evolution.dropfile.store.framework.single;

import com.evolution.dropfile.store.framework.CacheableKeyValueStore;

public class CacheableDefaultSingleValueStore<V>
        extends DefaultSingleValueStore<V>
        implements CacheableSingleValueStore<V> {

    public CacheableDefaultSingleValueStore(String storeName, CacheableKeyValueStore<V> store) {
        super(storeName, store);
    }

    @Override
    public void reset() {
        ((CacheableKeyValueStore) store).reset();
    }
}

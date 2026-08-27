package com.evolution.dropfile.store.framework.single;

import com.evolution.dropfile.store.framework.CacheableKeyValueStore;

public class CacheableDefaultSingleValueStore<V>
        extends DefaultSingleValueStore<V>
        implements CacheableSingleValueStore<V> {

    public CacheableDefaultSingleValueStore(CacheableKeyValueStore<V> store) {
        super(store);
    }

    @Override
    public void reset() {
        ((CacheableKeyValueStore) store).reset();
    }
}

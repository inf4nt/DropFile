package com.evolution.dropfile.store.framework.bootstrap;

import com.evolution.dropfile.store.framework.CacheableKeyValueStore;

public class CacheableDefaultBootstrapStore<V>
        extends DefaultBootstrapStore<V>
        implements CacheableBootstrapStore<V> {

    public CacheableDefaultBootstrapStore(String storeName,
                                          CacheableKeyValueStore<V> store) {
        super(storeName, store);
    }

    @Override
    public void reset() {
        ((CacheableKeyValueStore) store).reset();
    }
}

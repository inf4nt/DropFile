package com.evolution.dropfile.store.store;

public class DefaultKeyValueStoreInitializationProcedure
        implements KeyValueStoreInitializationProcedure<KeyValueStore> {
    @Override
    public void init(KeyValueStore keyValueStore) {
        keyValueStore.init();
    }
}

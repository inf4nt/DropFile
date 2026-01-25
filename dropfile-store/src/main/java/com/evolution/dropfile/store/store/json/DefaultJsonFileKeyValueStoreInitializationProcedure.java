package com.evolution.dropfile.store.store.json;

import com.evolution.dropfile.store.store.KeyValueStore;
import com.evolution.dropfile.store.store.KeyValueStoreInitializationProcedure;

@SuppressWarnings("rawtypes")
public class DefaultJsonFileKeyValueStoreInitializationProcedure
        implements KeyValueStoreInitializationProcedure {

    @Override
    public void init(KeyValueStore keyValueStore) {
        keyValueStore.getAll();
    }
}

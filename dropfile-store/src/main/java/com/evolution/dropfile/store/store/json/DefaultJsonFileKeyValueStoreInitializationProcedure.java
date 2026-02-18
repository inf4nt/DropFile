package com.evolution.dropfile.store.store.json;

import com.evolution.dropfile.store.store.KeyValueStore;
import com.evolution.dropfile.store.store.KeyValueStoreInitializationProcedure;

@Deprecated
@SuppressWarnings("rawtypes")
public class DefaultJsonFileKeyValueStoreInitializationProcedure
        implements KeyValueStoreInitializationProcedure {

    @Override
    public void init(KeyValueStore keyValueStore) {
        keyValueStore.init();
    }
}

package com.evolution.dropfile.store.store;

public interface KeyValueStoreInitializationProcedure<Store extends KeyValueStore> {

    void init(Store keyValueStore);
}

package com.evolution.dropfile.store.framework;

public interface KeyValueStoreInitializationProcedure<Store extends KeyValueStore> {

    void init(Store keyValueStore);
}

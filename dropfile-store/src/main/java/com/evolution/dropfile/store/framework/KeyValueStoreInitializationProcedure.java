package com.evolution.dropfile.store.framework;

public interface KeyValueStoreInitializationProcedure<Store extends KeyValueStore> {

    Class<Store> getStoreClass();

    void init(Store keyValueStore);
}

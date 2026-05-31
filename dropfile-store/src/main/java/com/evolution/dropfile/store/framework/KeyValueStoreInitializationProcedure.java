package com.evolution.dropfile.store.framework;

public interface KeyValueStoreInitializationProcedure<Store extends KeyValueStore> {

    default Class<Store> getStoreClass() {
        throw new UnsupportedOperationException();
    }

    void init(Store keyValueStore);
}

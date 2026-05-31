package com.evolution.dropfile.store.framework.single;

public interface SingleValueStoreInitializationProcedure<Store extends SingleValueStore> {

    default Class<Store> getStoreClass() {
        throw new UnsupportedOperationException();
    }

    void init(Store store);
}

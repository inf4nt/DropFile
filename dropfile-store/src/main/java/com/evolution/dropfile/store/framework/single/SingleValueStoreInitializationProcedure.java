package com.evolution.dropfile.store.framework.single;

public interface SingleValueStoreInitializationProcedure<Store extends SingleValueStore> {

    Class<Store> getStoreClass();

    void init(Store store);
}

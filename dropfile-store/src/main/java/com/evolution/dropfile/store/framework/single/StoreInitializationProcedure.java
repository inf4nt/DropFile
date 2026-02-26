package com.evolution.dropfile.store.framework.single;

public interface StoreInitializationProcedure<Store extends SingleValueStore> {

    void init(Store store);
}

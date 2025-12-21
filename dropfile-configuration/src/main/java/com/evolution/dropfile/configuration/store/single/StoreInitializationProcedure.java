package com.evolution.dropfile.configuration.store.single;

public interface StoreInitializationProcedure<Store extends SingleValueStore> {

    void init(Store store);
}

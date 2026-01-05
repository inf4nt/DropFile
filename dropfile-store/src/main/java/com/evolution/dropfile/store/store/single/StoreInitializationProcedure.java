package com.evolution.dropfile.store.store.single;

public interface StoreInitializationProcedure<Store extends SingleValueStore> {

    void init(Store store);
}

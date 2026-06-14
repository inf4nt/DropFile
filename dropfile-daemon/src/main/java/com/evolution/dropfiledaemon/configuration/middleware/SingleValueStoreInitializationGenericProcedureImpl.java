package com.evolution.dropfiledaemon.configuration.middleware;

import com.evolution.dropfile.store.framework.single.SingleValueStore;
import com.evolution.dropfile.store.framework.single.SingleValueStoreInitializationGenericProcedure;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SingleValueStoreInitializationGenericProcedureImpl
        implements SingleValueStoreInitializationGenericProcedure {

    @Override
    public void init(SingleValueStore<?> store) {
        String storeName = store.getClass().getSimpleName();
        log.info("Initializing store {}", storeName);
        store.init();
        log.info("Initialized store {}", storeName);

        log.info("Fetching data to trigger its logic {}", storeName);
        var object = store.get();
        log.info("{} triggered record exists {}", storeName, object.isPresent());
    }
}

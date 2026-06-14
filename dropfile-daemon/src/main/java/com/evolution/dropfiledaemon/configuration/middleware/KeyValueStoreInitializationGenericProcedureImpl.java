package com.evolution.dropfiledaemon.configuration.middleware;

import com.evolution.dropfile.store.framework.KeyValueStore;
import com.evolution.dropfile.store.framework.KeyValueStoreInitializationGenericProcedure;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KeyValueStoreInitializationGenericProcedureImpl
        implements KeyValueStoreInitializationGenericProcedure {

    @Override
    public void init(KeyValueStore<?> store) {
        String storeName = store.getClass().getSimpleName();
        log.info("Initializing {}", storeName);
        store.init();
        log.info("Initialized {}", storeName);

        log.info("Fetching data to trigger its logic {}", storeName);
        var object = store.getAll();
        log.info("{} triggered records {}", storeName, object.size());
    }
}

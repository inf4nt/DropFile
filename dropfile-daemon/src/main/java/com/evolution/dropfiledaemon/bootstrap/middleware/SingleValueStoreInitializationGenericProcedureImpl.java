package com.evolution.dropfiledaemon.bootstrap.middleware;

import com.evolution.dropfile.store.framework.single.SingleValueStore;
import com.evolution.dropfile.store.framework.single.SingleValueStoreInitializationGenericProcedure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class SingleValueStoreInitializationGenericProcedureImpl
        implements SingleValueStoreInitializationGenericProcedure {

    private final List<SingleValueStore> stores;

    @Override
    public void init() {
        for (SingleValueStore store : stores) {
            String storeName = store.getClass().getSimpleName();
            log.info("Initializing store {}", storeName);
            store.init();
            log.info("Initialized store {}", storeName);

            log.info("Fetching data to trigger its logic {}", storeName);
            var object = store.get();
            log.info("{} triggered record exists {}", storeName, object.isPresent());
        }
    }
}

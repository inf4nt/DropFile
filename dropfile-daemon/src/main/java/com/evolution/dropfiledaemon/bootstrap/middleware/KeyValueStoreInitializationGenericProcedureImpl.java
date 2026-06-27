package com.evolution.dropfiledaemon.bootstrap.middleware;

import com.evolution.dropfile.store.framework.KeyValueStore;
import com.evolution.dropfile.store.framework.KeyValueStoreInitializationGenericProcedure;
import com.evolution.dropfile.store.framework.KeyValueStoreInitializationProcedure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class KeyValueStoreInitializationGenericProcedureImpl
        implements KeyValueStoreInitializationGenericProcedure {

    private final List<KeyValueStore> stores;

    @Override
    public void init() {
        for (KeyValueStore store : stores) {
            String storeName = store.getClass().getSimpleName();
            log.info("Initializing {}", storeName);
            store.init();
            log.info("Initialized {}", storeName);

            log.info("Fetching data to trigger its logic {}", storeName);
            var object = store.getAll();
            log.info("{} triggered records {}", storeName, object.size());
        }
    }
}

package com.evolution.dropfiledaemon.bootstrap.procedure;

import com.evolution.dropfile.store.framework.KeyValueStore;
import com.evolution.dropfile.store.framework.KeyValueStoreInitializationGenericProcedure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Profile("prod")
@Component
@RequiredArgsConstructor
@Slf4j
public class KeyValueStoreInitializationGenericProcedureImpl
        implements KeyValueStoreInitializationGenericProcedure {

    private final List<KeyValueStore> stores;

    @Override
    public void init() {
        for (KeyValueStore store : stores) {
            String storeName = AopUtils.getTargetClass(store).getSimpleName();
            log.info("Fetching data to trigger its logic {}", storeName);
            var object = store.getAll();
            log.info("{} triggered records {}", storeName, object.size());
        }
    }
}

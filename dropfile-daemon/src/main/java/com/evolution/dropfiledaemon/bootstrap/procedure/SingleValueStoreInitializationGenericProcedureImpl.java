package com.evolution.dropfiledaemon.bootstrap.procedure;

import com.evolution.dropfile.store.framework.single.SingleValueStore;
import com.evolution.dropfile.store.framework.single.SingleValueStoreInitializationGenericProcedure;
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
public class SingleValueStoreInitializationGenericProcedureImpl
        implements SingleValueStoreInitializationGenericProcedure {

    private final List<SingleValueStore> stores;

    @Override
    public void init() {
        for (SingleValueStore store : stores) {
            String storeName = AopUtils.getTargetClass(store).getSimpleName();
            log.info("Fetching data to trigger its logic {}", storeName);
            var object = store.get();
            log.info("{} triggered record exists {}", storeName, object.isPresent());
        }
    }
}

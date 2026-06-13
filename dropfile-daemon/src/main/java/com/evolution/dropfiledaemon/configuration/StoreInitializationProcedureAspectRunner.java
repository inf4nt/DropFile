package com.evolution.dropfiledaemon.configuration;

import com.evolution.dropfile.store.framework.KeyValueStore;
import com.evolution.dropfile.store.framework.KeyValueStoreInitializationProcedure;
import com.evolution.dropfile.store.framework.single.SingleValueStore;
import com.evolution.dropfile.store.framework.single.SingleValueStoreInitializationProcedure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Aspect
@Component
public class StoreInitializationProcedureAspectRunner {

    private boolean initialized = false;

    private final List<KeyValueStore> keyValueStores;

    private final List<KeyValueStoreInitializationProcedure> keyValueStoreInitializationProcedures;

    private final List<SingleValueStore> singleValueStores;

    private final List<SingleValueStoreInitializationProcedure> singleValueStoreInitializationProcedures;

    private final ApplicationEventPublisher eventPublisher;

    @EventListener(ApplicationReadyEvent.class)
    public void applicationReadyEventListener() {
        // TODO If there is no init procedure, then keyValueStore must go through default procedure

        for (KeyValueStoreInitializationProcedure procedure : keyValueStoreInitializationProcedures) {
            log.info("Initializing key-value store {}", procedure.getStoreClass());
            KeyValueStore store = getKeyValueStore(procedure);
            procedure.init(store);
        }

        for (SingleValueStoreInitializationProcedure procedure : singleValueStoreInitializationProcedures) {
            log.info("Initializing single-value store {}", procedure.getStoreClass());
            SingleValueStore store = getSingleValueStore(procedure);
            procedure.init(store);
        }

        initialized = true;
        log.info("Store initialization completed");
        eventPublisher.publishEvent(new StoreInitializationProcedureReadyEvent());
    }

    @Before("execution(* com.evolution.dropfile.store.framework.KeyValueStore.*(..)) || " +
            "execution(* com.evolution.dropfile.store.framework.single.SingleValueStore.*(..))")
    public void checkAccess() {
        if (initialized) {
            return;
        }

        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        boolean approvedByProcedure = false;

        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            try {
                Class<?> clazz = Class.forName(className);
                if (KeyValueStoreInitializationProcedure.class.isAssignableFrom(clazz)
                        || SingleValueStoreInitializationProcedure.class.isAssignableFrom(clazz)) {
                    approvedByProcedure = true;
                    break;
                }
            } catch (ClassNotFoundException e) {
            }
        }

        if (!approvedByProcedure) {
            throw new IllegalStateException("Locked! Stores have not initialized yet");
        }
    }

    private KeyValueStore getKeyValueStore(KeyValueStoreInitializationProcedure procedure) {
        Class storeClass = procedure.getStoreClass();
        return keyValueStores.stream()
                .filter(it -> storeClass.isAssignableFrom(it.getClass()))
                .findFirst()
                .orElseThrow();
    }

    private SingleValueStore getSingleValueStore(SingleValueStoreInitializationProcedure procedure) {
        Class storeClass = procedure.getStoreClass();
        return singleValueStores.stream()
                .filter(it -> storeClass.isAssignableFrom(it.getClass()))
                .findFirst()
                .orElseThrow();
    }
}

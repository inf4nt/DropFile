package com.evolution.dropfiledaemon.bootstrap;

import com.evolution.dropfile.store.framework.KeyValueStoreInitializationGenericProcedure;
import com.evolution.dropfile.store.framework.KeyValueStoreInitializationProcedure;
import com.evolution.dropfile.store.framework.bootstrap.BootstrapStoreInitializationProcedure;
import com.evolution.dropfile.store.framework.single.SingleValueStoreInitializationGenericProcedure;
import com.evolution.dropfile.store.framework.single.SingleValueStoreInitializationProcedure;
import com.evolution.dropfiledaemon.bootstrap.event.DropFileDaemonApplicationReadyEvent;
import com.evolution.dropfiledaemon.bootstrap.event.DropFileDaemonBeforeApplicationReadyEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class StoreInitializationProcedureRunner {

    private final List<BootstrapStoreInitializationProcedure> bootstrapStoreInitializationProcedures;

    private final KeyValueStoreInitializationGenericProcedure keyValueStoreInitializationGenericProcedure;

    private final SingleValueStoreInitializationGenericProcedure singleValueStoreInitializationGenericProcedure;

    private final List<KeyValueStoreInitializationProcedure> keyValueStoreInitializationProcedureList;

    private final List<SingleValueStoreInitializationProcedure> singleValueStoreInitializationProcedures;

    private final ApplicationEventPublisher applicationEventPublisher;

    @EventListener(ApplicationReadyEvent.class)
    public void listener() {
        bootstrapStoreInitializationProcedures.forEach(it -> it.init());

        keyValueStoreInitializationGenericProcedure.init();
        singleValueStoreInitializationGenericProcedure.init();

        keyValueStoreInitializationProcedureList.forEach(it -> it.init());
        singleValueStoreInitializationProcedures.forEach(it -> it.init());

        applicationEventPublisher.publishEvent(new DropFileDaemonBeforeApplicationReadyEvent());
        applicationEventPublisher.publishEvent(new DropFileDaemonApplicationReadyEvent());
    }
}

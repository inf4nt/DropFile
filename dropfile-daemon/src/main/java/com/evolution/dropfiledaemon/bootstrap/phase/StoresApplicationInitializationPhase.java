package com.evolution.dropfiledaemon.bootstrap.phase;

import com.evolution.dropfile.store.framework.KeyValueStoreInitializationGenericProcedure;
import com.evolution.dropfile.store.framework.KeyValueStoreInitializationProcedure;
import com.evolution.dropfile.store.framework.single.SingleValueStoreInitializationGenericProcedure;
import com.evolution.dropfile.store.framework.single.SingleValueStoreInitializationProcedure;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class StoresApplicationInitializationPhase implements ApplicationInitializationPhase {

    private final List<KeyValueStoreInitializationGenericProcedure> keyValueStoreInitializationGenericProcedures;

    private final List<SingleValueStoreInitializationGenericProcedure> singleValueStoreInitializationGenericProcedures;

    private final List<KeyValueStoreInitializationProcedure> keyValueStoreInitializationProcedures;

    private final List<SingleValueStoreInitializationProcedure> singleValueStoreInitializationProcedures;

    @Override
    public void execute() throws Exception {
        keyValueStoreInitializationGenericProcedures.forEach(it -> it.init());
        singleValueStoreInitializationGenericProcedures.forEach(it -> it.init());

        keyValueStoreInitializationProcedures.forEach(it -> it.init());
        singleValueStoreInitializationProcedures.forEach(it -> it.init());
    }
}

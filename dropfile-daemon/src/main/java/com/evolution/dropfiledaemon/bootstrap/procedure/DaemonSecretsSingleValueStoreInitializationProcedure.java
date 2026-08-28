package com.evolution.dropfiledaemon.bootstrap.procedure;

import com.evolution.dropfile.store.framework.single.SingleValueStoreInitializationProcedure;
import com.evolution.dropfile.store.secret.DaemonSecret;
import com.evolution.dropfile.store.secret.DaemonSecretStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DaemonSecretsSingleValueStoreInitializationProcedure
        implements SingleValueStoreInitializationProcedure {

    private final DaemonSecretStore store;

    @Override
    public void init() {
        DaemonSecret value = new DaemonSecret(UUID.randomUUID().toString());
        store.save(value);
    }
}

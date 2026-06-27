package com.evolution.dropfiledaemon.bootstrap.middleware;

import com.evolution.dropfile.store.framework.single.SingleValueStoreInitializationProcedure;
import com.evolution.dropfile.store.secret.DaemonSecrets;
import com.evolution.dropfile.store.secret.DaemonSecretsStore;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class DaemonSecretsSingleValueStoreInitializationProcedure
        implements SingleValueStoreInitializationProcedure {

    private final DaemonSecretsStore store;

    @Override
    public void init() {
        DaemonSecrets value = new DaemonSecrets(UUID.randomUUID().toString());
        store.save(value);
    }
}

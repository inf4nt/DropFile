package com.evolution.dropfiledaemon.configuration.middleware;

import com.evolution.dropfile.store.framework.single.SingleValueStoreInitializationProcedure;
import com.evolution.dropfile.store.secret.DaemonSecrets;
import com.evolution.dropfile.store.secret.DaemonSecretsStore;

import java.util.UUID;

public class DaemonSecretsSingleValueStoreInitializationProcedure
        implements SingleValueStoreInitializationProcedure<DaemonSecretsStore> {

    @Override
    public void init(DaemonSecretsStore store) {
        DaemonSecrets value = new DaemonSecrets(UUID.randomUUID().toString());
        store.save(value);
    }
}

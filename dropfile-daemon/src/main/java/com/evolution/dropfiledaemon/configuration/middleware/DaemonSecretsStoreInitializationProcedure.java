package com.evolution.dropfiledaemon.configuration.middleware;

import com.evolution.dropfile.store.framework.single.StoreInitializationProcedure;
import com.evolution.dropfile.store.secret.DaemonSecrets;
import com.evolution.dropfile.store.secret.DaemonSecretsStore;

import java.util.UUID;

public class DaemonSecretsStoreInitializationProcedure
        implements StoreInitializationProcedure<DaemonSecretsStore> {

    @Override
    public void init(DaemonSecretsStore store) {
        store.init();

        DaemonSecrets value = new DaemonSecrets(UUID.randomUUID().toString());
        store.save(value);
    }
}

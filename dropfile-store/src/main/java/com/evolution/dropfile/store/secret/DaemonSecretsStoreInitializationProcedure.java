package com.evolution.dropfile.store.secret;

import com.evolution.dropfile.store.framework.single.StoreInitializationProcedure;

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

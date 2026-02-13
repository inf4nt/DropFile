package com.evolution.dropfile.store.secret;

import com.evolution.dropfile.store.store.single.StoreInitializationProcedure;

import java.util.UUID;

public class SecretsConfigStoreInitializationProcedure
        implements StoreInitializationProcedure<SecretsConfigStore> {

    @Override
    public void init(SecretsConfigStore store) {
        store.init();

        SecretsConfig config = new SecretsConfig(UUID.randomUUID().toString());
        store.save(config);
    }
}

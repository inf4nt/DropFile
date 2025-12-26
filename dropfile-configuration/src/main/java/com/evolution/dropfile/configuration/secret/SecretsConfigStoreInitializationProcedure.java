package com.evolution.dropfile.configuration.secret;

import com.evolution.dropfile.configuration.store.single.StoreInitializationProcedure;

import java.util.UUID;

public class SecretsConfigStoreInitializationProcedure
        implements StoreInitializationProcedure<SecretsConfigStore> {

    @Override
    public void init(SecretsConfigStore store) {
        SecretsConfig config = new SecretsConfig(UUID.randomUUID().toString());
        store.save(config);
    }
}

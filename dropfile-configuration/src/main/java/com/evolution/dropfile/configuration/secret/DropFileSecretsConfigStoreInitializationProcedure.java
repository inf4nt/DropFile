package com.evolution.dropfile.configuration.secret;

import com.evolution.dropfile.configuration.store.single.StoreInitializationProcedure;

import java.util.UUID;

public class DropFileSecretsConfigStoreInitializationProcedure
        implements StoreInitializationProcedure<DropFileSecretsConfigStore> {

    @Override
    public void init(DropFileSecretsConfigStore store) {
        DropFileSecretsConfig config = new DropFileSecretsConfig(UUID.randomUUID().toString());
        store.save(config);
    }
}

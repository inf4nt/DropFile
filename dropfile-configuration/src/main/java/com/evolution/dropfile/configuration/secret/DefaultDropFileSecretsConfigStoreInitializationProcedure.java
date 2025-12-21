package com.evolution.dropfile.configuration.secret;

import java.util.UUID;

public class DefaultDropFileSecretsConfigStoreInitializationProcedure
        implements DropFileSecretsConfigStoreInitializationProcedure {
    @Override
    public void init(DropFileSecretsConfigStore store) {
        DropFileSecretsConfig config = new DropFileSecretsConfig(UUID.randomUUID().toString());
        store.save(config);
    }
}

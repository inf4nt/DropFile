package com.evolution.dropfile.configuration.secret;

import com.evolution.dropfile.configuration.store.single.DefaultSingleValueStore;
import com.evolution.dropfile.configuration.store.KeyValueStore;

public class DefaultDropFileSecretsConfigStore
        extends DefaultSingleValueStore<DropFileSecretsConfig>
        implements DropFileSecretsConfigStore {

    private static final String STORE_NAME = "secrets_config";

    public DefaultDropFileSecretsConfigStore(KeyValueStore<String, DropFileSecretsConfig> store) {
        super(STORE_NAME, store);
    }
}

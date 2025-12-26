package com.evolution.dropfile.configuration.keys;

import com.evolution.dropfile.configuration.store.KeyValueStore;
import com.evolution.dropfile.configuration.store.single.DefaultSingleValueStore;

public class DefaultKeysConfigStore
        extends DefaultSingleValueStore<KeysConfig>
        implements KeysConfigStore {

    private static final String STORE_NAME = "keys_config";

    public DefaultKeysConfigStore(KeyValueStore<String, KeysConfig> store) {
        super(STORE_NAME, store);
    }
}

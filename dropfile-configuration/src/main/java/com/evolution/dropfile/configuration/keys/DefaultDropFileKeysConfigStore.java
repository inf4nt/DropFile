package com.evolution.dropfile.configuration.keys;

import com.evolution.dropfile.configuration.store.single.DefaultSingleValueStore;
import com.evolution.dropfile.configuration.store.KeyValueStore;

public class DefaultDropFileKeysConfigStore
        extends DefaultSingleValueStore<DropFileKeysConfig>
        implements DropFileKeysConfigStore {

    private static final String STORE_NAME = "keys_config";

    public DefaultDropFileKeysConfigStore(KeyValueStore<String, DropFileKeysConfig> store) {
        super(STORE_NAME, store);
    }
}

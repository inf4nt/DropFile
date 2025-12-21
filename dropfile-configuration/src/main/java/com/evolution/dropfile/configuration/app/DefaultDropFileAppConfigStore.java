package com.evolution.dropfile.configuration.app;

import com.evolution.dropfile.configuration.store.single.DefaultSingleValueStore;
import com.evolution.dropfile.configuration.store.KeyValueStore;

public class DefaultDropFileAppConfigStore
        extends DefaultSingleValueStore<DropFileAppConfig>
        implements DropFileAppConfigStore {

    private static final String STORE_NAME = "app_config";

    public DefaultDropFileAppConfigStore(KeyValueStore<String, DropFileAppConfig> store) {
        super(STORE_NAME, store);
    }
}

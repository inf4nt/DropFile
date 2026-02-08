package com.evolution.dropfile.store.keys;

import com.evolution.dropfile.store.store.KeyValueStore;
import com.evolution.dropfile.store.store.single.DefaultSingleValueStore;

public class DefaultKeysConfigStore
        extends DefaultSingleValueStore<KeysConfig>
        implements KeysConfigStore {

    public DefaultKeysConfigStore(KeyValueStore<KeysConfig> store) {
        super("keysConfig", store);
    }
}

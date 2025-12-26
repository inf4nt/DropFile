package com.evolution.dropfile.configuration.keys;

import com.evolution.dropfile.configuration.store.RuntimeKeyValueStore;

public class RuntimeKeysConfigStore
        extends DefaultKeysConfigStore {

    public RuntimeKeysConfigStore() {
        super(new RuntimeKeyValueStore<>());
    }
}

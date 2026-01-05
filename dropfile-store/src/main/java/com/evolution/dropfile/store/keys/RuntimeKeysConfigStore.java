package com.evolution.dropfile.store.keys;

import com.evolution.dropfile.store.store.RuntimeKeyValueStore;

public class RuntimeKeysConfigStore
        extends DefaultKeysConfigStore {

    public RuntimeKeysConfigStore() {
        super(new RuntimeKeyValueStore<>());
    }
}

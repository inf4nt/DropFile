package com.evolution.dropfile.configuration.keys;

import com.evolution.dropfile.configuration.store.RuntimeKeyValueStore;

public class RuntimeDropFileKeysConfigStore
        extends DefaultDropFileKeysConfigStore {

    public RuntimeDropFileKeysConfigStore() {
        super(new RuntimeKeyValueStore<>());
    }
}

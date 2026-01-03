package com.evolution.dropfile.configuration.access;

import com.evolution.dropfile.configuration.store.KeyValueStore;

public interface AccessKeyStore
        extends KeyValueStore<String, AccessKey> {
}

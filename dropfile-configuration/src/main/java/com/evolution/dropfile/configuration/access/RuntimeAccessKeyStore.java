package com.evolution.dropfile.configuration.access;

import com.evolution.dropfile.configuration.store.RuntimeKeyValueStore;

public class RuntimeAccessKeyStore
        extends RuntimeKeyValueStore<String, AccessKey>
        implements AccessKeyStore {
}

package com.evolution.dropfile.store.secret;

import com.evolution.dropfile.store.framework.CacheableKeyValueStore;
import com.evolution.dropfile.store.framework.single.CacheableDefaultSingleValueStore;

public class DaemonSecretsStoreImpl
        extends CacheableDefaultSingleValueStore<DaemonSecrets>
        implements DaemonSecretsStore {

    public DaemonSecretsStoreImpl(CacheableKeyValueStore<DaemonSecrets> store) {
        super(
                "daemonSecrets",
                store
        );
    }
}

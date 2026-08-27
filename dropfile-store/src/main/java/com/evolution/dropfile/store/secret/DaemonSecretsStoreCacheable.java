package com.evolution.dropfile.store.secret;

import com.evolution.dropfile.store.framework.CacheableKeyValueStore;
import com.evolution.dropfile.store.framework.single.CacheableDefaultSingleValueStore;

public class DaemonSecretsStoreCacheable
        extends CacheableDefaultSingleValueStore<DaemonSecrets>
        implements DaemonSecretsStore {

    public DaemonSecretsStoreCacheable(CacheableKeyValueStore<DaemonSecrets> store) {
        super(
                "daemonSecrets",
                store
        );
    }
}

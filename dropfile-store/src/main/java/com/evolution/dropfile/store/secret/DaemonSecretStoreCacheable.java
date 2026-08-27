package com.evolution.dropfile.store.secret;

import com.evolution.dropfile.store.framework.CacheableKeyValueStore;
import com.evolution.dropfile.store.framework.single.CacheableDefaultSingleValueStore;

public class DaemonSecretStoreCacheable
        extends CacheableDefaultSingleValueStore<DaemonSecret>
        implements DaemonSecretStore {

    public DaemonSecretStoreCacheable(CacheableKeyValueStore<DaemonSecret> store) {
        super(
                DaemonSecretStore.class.getSimpleName(),
                store
        );
    }
}

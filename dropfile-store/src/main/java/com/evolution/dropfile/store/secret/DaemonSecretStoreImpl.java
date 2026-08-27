package com.evolution.dropfile.store.secret;

import com.evolution.dropfile.store.framework.KeyValueStore;
import com.evolution.dropfile.store.framework.single.DefaultSingleValueStore;

public class DaemonSecretStoreImpl
        extends DefaultSingleValueStore<DaemonSecret>
        implements DaemonSecretStore {

    public DaemonSecretStoreImpl(KeyValueStore<DaemonSecret> store) {
        super(
                DaemonSecretStore.class.getSimpleName(),
                store
        );
    }
}

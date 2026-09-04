package com.evolution.dropfile.store.secret;

import com.evolution.dropfile.store.framework.KeyValueStore;
import com.evolution.dropfile.store.framework.single.DefaultSingleValueStore;

public class DefaultDaemonSecretStore
        extends DefaultSingleValueStore<DaemonSecret>
        implements DaemonSecretStore {

    public DefaultDaemonSecretStore(KeyValueStore<DaemonSecret> store) {
        super(store);
    }
}

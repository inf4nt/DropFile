package com.evolution.dropfile.store.secret;

import com.evolution.dropfile.store.framework.KeyValueStore;
import com.evolution.dropfile.store.framework.single.DefaultSingleValueStore;

public class DaemonSecretsStoreImpl
        extends DefaultSingleValueStore<DaemonSecrets>
        implements DaemonSecretsStore {

    public DaemonSecretsStoreImpl(KeyValueStore<DaemonSecrets> store) {
        super(
                "daemonSecrets",
                store
        );
    }
}

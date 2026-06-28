package com.evolution.dropfile.store.secret;

import com.evolution.dropfile.store.framework.single.ImmutableSingleValueStore;

public class ImmutableDaemonSecretsStore
        extends ImmutableSingleValueStore<DaemonSecrets>
        implements DaemonSecretsStore {

    public ImmutableDaemonSecretsStore(DaemonSecrets value) {
        super(value);
    }
}

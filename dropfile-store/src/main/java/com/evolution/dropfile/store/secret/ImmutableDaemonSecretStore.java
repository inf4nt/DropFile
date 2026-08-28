package com.evolution.dropfile.store.secret;

import com.evolution.dropfile.store.framework.single.ImmutableSingleValueStore;

@Deprecated
public class ImmutableDaemonSecretStore
        extends ImmutableSingleValueStore<DaemonSecret>
        implements DaemonSecretStore {

    public ImmutableDaemonSecretStore(DaemonSecret value) {
        super(value);
    }
}

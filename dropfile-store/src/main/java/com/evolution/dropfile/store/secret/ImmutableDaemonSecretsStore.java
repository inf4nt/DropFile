package com.evolution.dropfile.store.secret;

import com.evolution.dropfile.store.framework.single.ImmutableSingleValueStore;

import java.util.function.Supplier;

public class ImmutableDaemonSecretsStore
        extends ImmutableSingleValueStore<DaemonSecrets>
        implements DaemonSecretsStore {

    public ImmutableDaemonSecretsStore(Supplier<DaemonSecrets> value) {
        super(value.get());
    }
}

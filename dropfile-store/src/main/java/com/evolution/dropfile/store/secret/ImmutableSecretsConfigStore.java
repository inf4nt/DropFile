package com.evolution.dropfile.store.secret;

import com.evolution.dropfile.store.store.single.ImmutableSingleValueStore;

import java.util.function.Supplier;

public class ImmutableSecretsConfigStore
        extends ImmutableSingleValueStore<SecretsConfig>
        implements SecretsConfigStore {

    public ImmutableSecretsConfigStore(Supplier<SecretsConfig> value) {
        super(value.get());
    }
}

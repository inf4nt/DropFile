package com.evolution.dropfile.configuration.secret;

import com.evolution.dropfile.configuration.store.single.ImmutableSingleValueStore;

import java.util.function.Supplier;

public class ImmutableSecretsConfigStore
        extends ImmutableSingleValueStore<SecretsConfig>
        implements SecretsConfigStore {

    public ImmutableSecretsConfigStore(Supplier<SecretsConfig> value) {
        super(value.get());
    }
}

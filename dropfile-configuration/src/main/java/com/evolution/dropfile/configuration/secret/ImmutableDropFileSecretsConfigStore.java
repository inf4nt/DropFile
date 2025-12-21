package com.evolution.dropfile.configuration.secret;

import com.evolution.dropfile.configuration.store.single.ImmutableSingleValueStore;

import java.util.function.Supplier;

public class ImmutableDropFileSecretsConfigStore
        extends ImmutableSingleValueStore<DropFileSecretsConfig>
        implements DropFileSecretsConfigStore {

    public ImmutableDropFileSecretsConfigStore(Supplier<DropFileSecretsConfig> value) {
        super(value.get());
    }
}

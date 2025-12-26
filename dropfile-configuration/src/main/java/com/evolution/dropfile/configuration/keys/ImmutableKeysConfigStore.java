package com.evolution.dropfile.configuration.keys;

import com.evolution.dropfile.configuration.store.single.ImmutableSingleValueStore;

import java.util.function.Supplier;

public class ImmutableKeysConfigStore
        extends ImmutableSingleValueStore<KeysConfig>
        implements KeysConfigStore {
    public ImmutableKeysConfigStore(Supplier<KeysConfig> value) {
        super(value.get());
    }
}

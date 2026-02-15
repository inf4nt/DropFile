package com.evolution.dropfile.store.keys;

import com.evolution.dropfile.store.store.single.ImmutableSingleValueStore;

import java.util.function.Supplier;

@Deprecated
public class ImmutableKeysConfigStore
        extends ImmutableSingleValueStore<KeysConfig>
        implements KeysConfigStore {
    public ImmutableKeysConfigStore(Supplier<KeysConfig> value) {
        super(value.get());
    }
}

package com.evolution.dropfile.configuration.keys;

import com.evolution.dropfile.configuration.store.single.ImmutableSingleValueStore;

import java.util.function.Supplier;

public class ImmutableDropFileKeysConfigStore
        extends ImmutableSingleValueStore<DropFileKeysConfig>
        implements DropFileKeysConfigStore {
    public ImmutableDropFileKeysConfigStore(Supplier<DropFileKeysConfig> value) {
        super(value.get());
    }
}

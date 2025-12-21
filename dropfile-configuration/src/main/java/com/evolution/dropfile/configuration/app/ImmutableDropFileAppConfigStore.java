package com.evolution.dropfile.configuration.app;

import com.evolution.dropfile.configuration.store.single.ImmutableSingleValueStore;

import java.util.function.Supplier;

public class ImmutableDropFileAppConfigStore
        extends ImmutableSingleValueStore<DropFileAppConfig>
        implements DropFileAppConfigStore {

    public ImmutableDropFileAppConfigStore(Supplier<DropFileAppConfig> value) {
        super(value.get());
    }
}

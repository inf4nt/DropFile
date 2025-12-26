package com.evolution.dropfile.configuration.app;

import com.evolution.dropfile.configuration.store.single.ImmutableSingleValueStore;

import java.util.function.Supplier;

public class ImmutableAppConfigStore
        extends ImmutableSingleValueStore<AppConfig>
        implements AppConfigStore {

    public ImmutableAppConfigStore(Supplier<AppConfig> value) {
        super(value.get());
    }
}

package com.evolution.dropfile.store.app;

import com.evolution.dropfile.store.store.single.ImmutableSingleValueStore;

import java.util.function.Supplier;

public class ImmutableAppConfigStore
        extends ImmutableSingleValueStore<AppConfig>
        implements AppConfigStore {

    public ImmutableAppConfigStore(Supplier<AppConfig> value) {
        super(value.get());
    }
}

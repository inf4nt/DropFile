package com.evolution.dropfile.store.app.cli;

import com.evolution.dropfile.store.framework.single.ImmutableSingleValueStore;

import java.util.function.Supplier;

public class ImmutableCliAppConfigStore
        extends ImmutableSingleValueStore<CliAppConfig>
        implements CliAppConfigStore {

    public ImmutableCliAppConfigStore(Supplier<CliAppConfig> value) {
        super(value.get());
    }
}

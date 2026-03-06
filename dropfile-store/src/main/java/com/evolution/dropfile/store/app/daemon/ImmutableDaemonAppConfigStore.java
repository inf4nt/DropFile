package com.evolution.dropfile.store.app.daemon;

import com.evolution.dropfile.store.framework.single.ImmutableSingleValueStore;

import java.util.function.Supplier;

public class ImmutableDaemonAppConfigStore
        extends ImmutableSingleValueStore<DaemonAppConfig>
        implements DaemonAppConfigStore {

    public ImmutableDaemonAppConfigStore(Supplier<DaemonAppConfig> value) {
        super(value.get());
    }
}

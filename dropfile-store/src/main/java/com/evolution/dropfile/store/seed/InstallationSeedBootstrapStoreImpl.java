package com.evolution.dropfile.store.seed;

import com.evolution.dropfile.store.framework.CacheableKeyValueStore;
import com.evolution.dropfile.store.framework.bootstrap.CacheableDefaultBootstrapStore;

import java.util.UUID;

public class InstallationSeedBootstrapStoreImpl
        extends CacheableDefaultBootstrapStore<UUID>
        implements InstallationSeedBootstrapStore {

    public InstallationSeedBootstrapStoreImpl(CacheableKeyValueStore<UUID> store) {
        super("seed", store);
    }
}

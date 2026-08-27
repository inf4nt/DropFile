package com.evolution.dropfile.store.seed;

import com.evolution.dropfile.store.framework.CacheableKeyValueStore;
import com.evolution.dropfile.store.framework.bootstrap.CacheableDefaultBootstrapStore;

import java.util.UUID;

public class InstallationSeedBootstrapStoreCacheable
        extends CacheableDefaultBootstrapStore<UUID>
        implements InstallationSeedBootstrapStore {

    public InstallationSeedBootstrapStoreCacheable(CacheableKeyValueStore<UUID> store) {
        super("seed", store);
    }
}

package com.evolution.dropfile.store.app.daemon;

import com.evolution.dropfile.store.framework.single.CacheableSingleValueStore;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CacheableJsonFileDaemonAppConfigStore
        extends CacheableSingleValueStore<DaemonAppConfig>
        implements DaemonAppConfigStore {

    public CacheableJsonFileDaemonAppConfigStore(ObjectMapper objectMapper) {
        super(new JsonFileDaemonAppConfigStore(objectMapper));
    }
}

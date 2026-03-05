package com.evolution.dropfile.store.app;

import com.evolution.dropfile.store.framework.single.CacheableSingleValueStore;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CacheableJsonFileAppConfigStore
        extends CacheableSingleValueStore<AppConfig>
        implements AppConfigStore {

    public CacheableJsonFileAppConfigStore(ObjectMapper objectMapper) {
        super(new JsonFileAppConfigStore(objectMapper));
    }
}

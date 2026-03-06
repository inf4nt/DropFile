package com.evolution.dropfile.store.app.cli;

import com.evolution.dropfile.store.framework.single.CacheableSingleValueStore;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CacheableJsonFileCliAppConfigStore
        extends CacheableSingleValueStore<CliAppConfig>
        implements CliAppConfigStore {

    public CacheableJsonFileCliAppConfigStore(ObjectMapper objectMapper) {
        super(new JsonFileCliAppConfigStore(objectMapper));
    }
}

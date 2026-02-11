package com.evolution.dropfile.store.keys;

import com.evolution.dropfile.store.store.json.DefaultJsonSerde;
import com.evolution.dropfile.store.store.json.FileProtectedProvider;
import com.evolution.dropfile.store.store.json.JsonFileKeyValueStore;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonFileKeysConfigStore
        extends DefaultKeysConfigStore {

    public JsonFileKeysConfigStore(ObjectMapper objectMapper) {
        super(
                new JsonFileKeyValueStore<>(
                        new FileProtectedProvider() {
                            @Override
                            public String getFileName() {
                                return "keys.config.json";
                            }
                        },
                        new DefaultJsonSerde<>(
                                KeysConfig.class,
                                objectMapper
                        )
                )
        );
    }
}

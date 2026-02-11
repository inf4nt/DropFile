package com.evolution.dropfile.store.access;

import com.evolution.dropfile.store.store.json.DefaultJsonSerde;
import com.evolution.dropfile.store.store.json.FileProtectedProvider;
import com.evolution.dropfile.store.store.json.JsonFileKeyValueStore;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonFileAccessKeyStore
        extends JsonFileKeyValueStore<AccessKey>
        implements AccessKeyStore {

    public JsonFileAccessKeyStore(ObjectMapper objectMapper) {
        super(
                new FileProtectedProvider() {
                    @Override
                    public String getFileName() {
                        return "access.keys.config.json";
                    }
                },
                new DefaultJsonSerde<>(
                        AccessKey.class,
                        objectMapper
                )
        );
    }
}

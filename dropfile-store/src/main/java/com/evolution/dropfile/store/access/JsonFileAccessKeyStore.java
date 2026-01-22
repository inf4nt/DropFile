package com.evolution.dropfile.store.access;

import com.evolution.dropfile.store.store.json.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.nio.file.Paths;

public class JsonFileAccessKeyStore
        extends JsonFileKeyValueStore<AccessKey>
        implements AccessKeyStore {

    public JsonFileAccessKeyStore(ObjectMapper objectMapper) {
        super(
                new FileProtectedProvider() {
                    @Override
                    public Path getFilePath() {
                        return Paths.get("access.keys.config.json");
                    }
                },
                new DefaultJsonSerde<>(
                        AccessKey.class,
                        objectMapper
                )
        );
    }
}

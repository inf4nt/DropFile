package com.evolution.dropfile.configuration.keys;

import com.evolution.dropfile.configuration.store.json.DefaultJsonSerde;
import com.evolution.dropfile.configuration.store.json.FileProtectedProvider;
import com.evolution.dropfile.configuration.store.json.FileProvider;
import com.evolution.dropfile.configuration.store.json.JsonFileKeyValueStore;
import com.evolution.dropfile.configuration.store.single.DefaultSingleValueStore;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.nio.file.Paths;

public class JsonFileDropFileKeysConfigStore
        extends DefaultSingleValueStore<DropFileKeysConfig>
        implements DropFileKeysConfigStore {

    private static final String STORE_NAME = "keys_config";

    public JsonFileDropFileKeysConfigStore(ObjectMapper objectMapper) {
        super(
                STORE_NAME,
                new JsonFileKeyValueStore<>(
                        new FileProtectedProvider() {
                            @Override
                            public Path getFilePath() {
                                return Paths.get("keys.config.json");
                            }
                        },
                        new DefaultJsonSerde<>(
                                DropFileKeysConfig.class,
                                objectMapper
                        )
                )
        );
    }
}

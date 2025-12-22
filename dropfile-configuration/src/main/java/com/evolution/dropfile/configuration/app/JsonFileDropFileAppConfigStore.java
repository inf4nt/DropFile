package com.evolution.dropfile.configuration.app;

import com.evolution.dropfile.configuration.store.json.DefaultJsonSerde;
import com.evolution.dropfile.configuration.store.json.FileProvider;
import com.evolution.dropfile.configuration.store.json.JsonFileKeyValueStore;
import com.evolution.dropfile.configuration.store.single.DefaultSingleValueStore;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.nio.file.Paths;

public class JsonFileDropFileAppConfigStore
        extends DefaultSingleValueStore<DropFileAppConfig>
        implements DropFileAppConfigStore {

    private static final String STORE_NAME = "app_config";

    public JsonFileDropFileAppConfigStore(ObjectMapper objectMapper) {
        super(
                STORE_NAME,
                new JsonFileKeyValueStore<>(
                        new FileProvider() {
                            @Override
                            public Path getFilePath() {
                                return Paths.get("app.config.json");
                            }
                        },
                        new DefaultJsonSerde<>(
                                DropFileAppConfig.class,
                                objectMapper
                        )
                )
        );
    }
}

package com.evolution.dropfile.store.app;

import com.evolution.dropfile.store.store.json.DefaultJsonSerde;
import com.evolution.dropfile.store.store.json.FileProvider;
import com.evolution.dropfile.store.store.json.JsonFileKeyValueStore;
import com.evolution.dropfile.store.store.single.DefaultSingleValueStore;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.nio.file.Paths;

public class JsonFileAppConfigStore
        extends DefaultSingleValueStore<AppConfig>
        implements AppConfigStore {

    private static final String STORE_NAME = "appConfig";

    public JsonFileAppConfigStore(ObjectMapper objectMapper) {
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
                                AppConfig.class,
                                objectMapper
                        )
                )
        );
    }
}

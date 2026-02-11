package com.evolution.dropfile.store.app;

import com.evolution.dropfile.store.store.json.DefaultJsonSerde;
import com.evolution.dropfile.store.store.json.FileProvider;
import com.evolution.dropfile.store.store.json.JsonFileKeyValueStore;
import com.evolution.dropfile.store.store.single.DefaultSingleValueStore;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;

public class JsonFileAppConfigStore
        extends DefaultSingleValueStore<AppConfig>
        implements AppConfigStore {

    public JsonFileAppConfigStore(ObjectMapper objectMapper) {
        super(
                "appConfig",
                new JsonFileKeyValueStore<>(
                        new FileProvider() {
                            @Override
                            public String getFileName() {
                                return "app.config.json";
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

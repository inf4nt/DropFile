package com.evolution.dropfile.store.app;

import com.evolution.dropfile.store.store.file.FileProviderImpl;
import com.evolution.dropfile.store.store.file.JsonFileOperations;
import com.evolution.dropfile.store.store.file.SynchronizedFileKeyValueStore;
import com.evolution.dropfile.store.store.single.DefaultSingleValueStore;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonFileAppConfigStore
        extends DefaultSingleValueStore<AppConfig>
        implements AppConfigStore {

    public JsonFileAppConfigStore(ObjectMapper objectMapper) {
        super(
                "appConfig",
                new SynchronizedFileKeyValueStore<>(
                        new FileProviderImpl("app.config.json"),
                        new JsonFileOperations<>(
                                objectMapper,
                                AppConfig.class
                        )
                )
        );
    }
}

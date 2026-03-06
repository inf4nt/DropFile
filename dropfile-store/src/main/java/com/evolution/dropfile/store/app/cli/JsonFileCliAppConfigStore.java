package com.evolution.dropfile.store.app.cli;

import com.evolution.dropfile.store.framework.file.FileProviderImpl;
import com.evolution.dropfile.store.framework.file.JsonFileOperations;
import com.evolution.dropfile.store.framework.file.SynchronizedFileKeyValueStore;
import com.evolution.dropfile.store.framework.single.DefaultSingleValueStore;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonFileCliAppConfigStore
        extends DefaultSingleValueStore<CliAppConfig>
        implements CliAppConfigStore {

    public JsonFileCliAppConfigStore(ObjectMapper objectMapper) {
        super(
                "cliAppConfig",
                new SynchronizedFileKeyValueStore<>(
                        new FileProviderImpl("cli.app.config.json"),
                        new JsonFileOperations<>(
                                objectMapper,
                                CliAppConfig.class
                        )
                )
        );
    }
}

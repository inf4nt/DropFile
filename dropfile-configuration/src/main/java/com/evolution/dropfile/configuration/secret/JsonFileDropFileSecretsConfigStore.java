package com.evolution.dropfile.configuration.secret;

import com.evolution.dropfile.configuration.store.json.DefaultJsonSerde;
import com.evolution.dropfile.configuration.store.json.FileProtectedProvider;
import com.evolution.dropfile.configuration.store.json.JsonFileKeyValueStore;
import com.evolution.dropfile.configuration.store.single.DefaultSingleValueStore;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.nio.file.Paths;

public class JsonFileDropFileSecretsConfigStore
        extends DefaultSingleValueStore<DropFileSecretsConfig>
        implements DropFileSecretsConfigStore {

    private static final String STORE_NAME = "secrets_config";

    public JsonFileDropFileSecretsConfigStore(ObjectMapper objectMapper) {
        super(
                STORE_NAME,
                new JsonFileKeyValueStore<>(
                        new FileProtectedProvider() {
                            @Override
                            public Path getFilePath() {
                                return Paths.get("secrets.config.json");
                            }
                        },
                        new DefaultJsonSerde<>(
                                DropFileSecretsConfig.class,
                                objectMapper
                        )
                )
        );
    }
}

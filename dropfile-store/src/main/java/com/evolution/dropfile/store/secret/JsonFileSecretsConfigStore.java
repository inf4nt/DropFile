package com.evolution.dropfile.store.secret;

import com.evolution.dropfile.store.store.json.DefaultJsonSerde;
import com.evolution.dropfile.store.store.json.FileProtectedProvider;
import com.evolution.dropfile.store.store.json.JsonFileKeyValueStore;
import com.evolution.dropfile.store.store.single.DefaultSingleValueStore;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.nio.file.Paths;

public class JsonFileSecretsConfigStore
        extends DefaultSingleValueStore<SecretsConfig>
        implements SecretsConfigStore {

    public JsonFileSecretsConfigStore(ObjectMapper objectMapper) {
        super(
                "secretsConfig",
                new JsonFileKeyValueStore<>(
                        new FileProtectedProvider() {
                            @Override
                            public Path getFilePath() {
                                return Paths.get("secrets.config.json");
                            }
                        },
                        new DefaultJsonSerde<>(
                                SecretsConfig.class,
                                objectMapper
                        )
                )
        );
    }
}

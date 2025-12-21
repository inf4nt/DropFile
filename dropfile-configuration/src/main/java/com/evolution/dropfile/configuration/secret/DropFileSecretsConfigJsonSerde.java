package com.evolution.dropfile.configuration.secret;

import com.evolution.dropfile.configuration.store.json.DefaultJsonSerde;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DropFileSecretsConfigJsonSerde extends DefaultJsonSerde<DropFileSecretsConfig> {

    public DropFileSecretsConfigJsonSerde(ObjectMapper objectMapper) {
        super(DropFileSecretsConfig.class, objectMapper);
    }
}

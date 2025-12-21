package com.evolution.dropfile.configuration.app;

import com.evolution.dropfile.configuration.store.json.DefaultJsonSerde;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DropFileAppConfigJsonSerde extends DefaultJsonSerde<DropFileAppConfig> {

    public DropFileAppConfigJsonSerde(ObjectMapper objectMapper) {
        super(DropFileAppConfig.class, objectMapper);
    }
}

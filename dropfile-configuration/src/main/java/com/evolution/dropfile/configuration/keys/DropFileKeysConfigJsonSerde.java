package com.evolution.dropfile.configuration.keys;

import com.evolution.dropfile.configuration.store.json.DefaultJsonSerde;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DropFileKeysConfigJsonSerde extends DefaultJsonSerde<DropFileKeysConfig> {

    public DropFileKeysConfigJsonSerde(ObjectMapper objectMapper) {
        super(DropFileKeysConfig.class, objectMapper);
    }
}

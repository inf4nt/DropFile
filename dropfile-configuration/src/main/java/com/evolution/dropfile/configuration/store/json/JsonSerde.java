package com.evolution.dropfile.configuration.store.json;

import java.util.Map;

public interface JsonSerde<V> {

    byte[] serialize(Map<String, V> values);

    Map<String, V> deserialize(byte[] data);
}

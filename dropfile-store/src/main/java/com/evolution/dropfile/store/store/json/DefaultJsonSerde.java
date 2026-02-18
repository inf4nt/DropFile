package com.evolution.dropfile.store.store.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

@Deprecated
public class DefaultJsonSerde<V> implements JsonSerde<V> {

    private final TypeReference<Map<String, V>> typeReference;

    private final ObjectMapper objectMapper;

    public DefaultJsonSerde(Class<V> classType,
                            ObjectMapper objectMapper) {
        this.typeReference = new TypeReference<>() {
            @Override
            public Type getType() {
                return new ParameterizedType() {
                    @Override
                    public Type[] getActualTypeArguments() {
                        return List.of(String.class, classType).toArray(Type[]::new);
                    }

                    @Override
                    public Type getRawType() {
                        return Map.class;
                    }

                    @Override
                    public Type getOwnerType() {
                        return null;
                    }
                };
            }
        };
        this.objectMapper = objectMapper;
    }

    @SneakyThrows
    @Override
    public byte[] serialize(Map<String, V> values) {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(values);
    }

    @SneakyThrows
    @Override
    public Map<String, V> deserialize(byte[] data) {
        return objectMapper.readValue(data, typeReference);
    }
}

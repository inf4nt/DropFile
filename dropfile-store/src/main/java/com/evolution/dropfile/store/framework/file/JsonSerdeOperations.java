package com.evolution.dropfile.store.framework.file;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class JsonSerdeOperations<V> implements SerdeOperations<V> {

    private final ObjectMapper objectMapper;

    private final TypeReference<Map<String, V>> typeReference;

    public JsonSerdeOperations(ObjectMapper objectMapper, Class<V> valueClass) {
        this.objectMapper = objectMapper;
        this.typeReference = new TypeReference<>() {
            @Override
            public Type getType() {
                return new ParameterizedType() {
                    @Override
                    public Type[] getActualTypeArguments() {
                        return List.of(String.class, valueClass).toArray(Type[]::new);
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
    }

    @Override
    public Map<String, V> deserialize(byte[] bytes) throws IOException {
        return objectMapper.readValue(bytes, typeReference);
    }

    @Override
    public byte[] serialize(Map<String, V> values) throws IOException {
        return objectMapper.writeValueAsBytes(values);
    }
}

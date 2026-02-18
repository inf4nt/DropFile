package com.evolution.dropfile.store.store.file;

import com.evolution.dropfile.common.CommonFileUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JsonFileOperations<V> implements FileOperations<V> {

    protected final ObjectMapper objectMapper;

    protected final TypeReference<Map<String, V>> typeReference;

    public JsonFileOperations(ObjectMapper objectMapper, Class<V> classType) {
        this.objectMapper = objectMapper;
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
    }

    @SneakyThrows
    @Override
    public void removeAll(Path destination) {
        Path temporaryFilePath = null;
        try {
            temporaryFilePath = getOrCreateTemporaryFilePath(destination);
            Files.move(temporaryFilePath, destination, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            if (temporaryFilePath != null) {
                Files.deleteIfExists(temporaryFilePath);
            }
        }
    }

    @SneakyThrows
    @Override
    public void write(Path destination, Map<String, V> values) {
        Path temporaryFilePath = null;
        try {
            byte[] bytes = serialize(values);
            temporaryFilePath = getOrCreateTemporaryFilePath(destination);
            Files.write(temporaryFilePath, bytes);
            Files.move(temporaryFilePath, destination, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            if (temporaryFilePath != null) {
                Files.deleteIfExists(temporaryFilePath);
            }
        }
    }

    @SneakyThrows
    @Override
    public Map<String, V> read(Path destination) {
        if (Files.notExists(destination) || Files.size(destination) == 0) {
            return Collections.emptyMap();
        }
        byte[] bytes = Files.readAllBytes(destination);
        return deserialize(bytes);
    }

    @SneakyThrows
    protected Map<String, V> deserialize(byte[] bytes) {
        return objectMapper.readValue(bytes, typeReference);
    }

    @SneakyThrows
    protected byte[] serialize(Map<String, V> values) {
        return objectMapper.writeValueAsBytes(values);
    }

    @SneakyThrows
    protected Path getOrCreateTemporaryFilePath(Path destination) {
        String filename = destination.toFile().getName();
        String temporaryFileName = CommonFileUtils.getTemporaryFileName(filename);
        Path parent = destination.getParent();
        if (Files.notExists(parent)) {
            Files.createDirectories(parent);
        }
        Path temporaryFilePath = parent.resolve(temporaryFileName);
        if (Files.notExists(temporaryFilePath)) {
            Files.createFile(temporaryFilePath);
        }
        return temporaryFilePath;
    }
}

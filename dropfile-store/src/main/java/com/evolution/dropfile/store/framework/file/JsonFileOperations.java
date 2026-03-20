package com.evolution.dropfile.store.framework.file;

import com.evolution.dropfile.common.CommonFileUtils;
import com.evolution.dropfile.common.FileHelper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class JsonFileOperations<V> implements FileOperations<V> {

    protected final FileHelper fileHelper;

    protected final ObjectMapper objectMapper;

    protected final Class<V> classType;

    protected final TypeReference<Map<String, V>> typeReference;

    public JsonFileOperations(FileHelper fileHelper, ObjectMapper objectMapper, Class<V> classType) {
        this.fileHelper = fileHelper;
        this.objectMapper = objectMapper;
        this.classType = classType;
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
            fileHelper.write(temporaryFilePath, bytes);
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
        try (FileChannel fileChannel = FileChannel.open(destination, StandardOpenOption.READ);
             InputStream inputStream = Channels.newInputStream(fileChannel)) {
            return deserialize(inputStream);
        }
    }

    @SneakyThrows
    protected Map<String, V> deserialize(InputStream inputStream) {
        return objectMapper.readValue(inputStream, typeReference);
    }

    @SneakyThrows
    protected byte[] serialize(Map<String, V> values) {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(values);
    }

    @SneakyThrows
    private Path getOrCreateTemporaryFilePath(Path destination) {
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

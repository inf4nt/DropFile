package com.evolution.dropfile.store.store.json;

import com.evolution.dropfile.store.store.KeyValueStore;
import lombok.SneakyThrows;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class JsonFileKeyValueStore<V> implements KeyValueStore<V> {

    private final FileProvider fileProvider;

    private final JsonSerde<V> jsonSerde;

    public JsonFileKeyValueStore(FileProvider fileProvider,
                                 JsonSerde<V> jsonSerde) {
        this.fileProvider = fileProvider;
        this.jsonSerde = jsonSerde;
    }

    @Override
    public synchronized V save(String key, V value) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(value, "value cannot be null");
        validate(key, value);
        return saveKeyValue(key, value);
    }

    @Override
    public synchronized V remove(String key) {
        Objects.requireNonNull(key, "key cannot be null");
        return removeByKey(key);
    }

    @Override
    public synchronized void removeAll() {
        removeAllMethod();
    }

    @Override
    public synchronized Map<String, V> getAll() {
        return getAllMap();
    }

    private V saveKeyValue(String key, V value) {
        Map<String, V> all = new LinkedHashMap<>(getAll());
        all.put(key, value);
        byte[] allBytes = jsonSerde.serialize(all);
        writeData(allBytes);
        return value;
    }

    private V removeByKey(String key) {
        Map<String, V> all = new LinkedHashMap<>(getAll());
        V remove = all.remove(key);
        if (remove == null) {
            return null;
        }
        byte[] allBytes = jsonSerde.serialize(all);
        writeData(allBytes);
        return remove;
    }

    @SneakyThrows
    private void removeAllMethod() {
        File file = fileProvider.getOrCreateFile();
        if (Files.size(file.toPath()) == 0) {
            return;
        }
        writeData("{}".getBytes());
    }

    @SneakyThrows
    private Map<String, V> getAllMap() {
        File file = fileProvider.getOrCreateFile();
        
        if (Files.size(file.toPath()) == 0) {
            return Collections.emptyMap();
        }

        byte[] allBytes = Files.readAllBytes(file.toPath());
        return jsonSerde.deserialize(allBytes);
    }

    @SneakyThrows
    private void writeData(byte[] data) {
        File tmp = null;
        try {
            tmp = fileProvider.getOrCreateTempFile();
            Files.write(tmp.toPath(), data);
            Path filePath = fileProvider.getFilePath();
            Files.move(tmp.toPath(), filePath, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            if (tmp != null && Files.exists(tmp.toPath())) {
                Files.delete(tmp.toPath());
            }
        }
    }
}

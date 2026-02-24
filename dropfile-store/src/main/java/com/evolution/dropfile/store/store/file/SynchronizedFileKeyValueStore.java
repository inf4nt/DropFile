package com.evolution.dropfile.store.store.file;

import com.evolution.dropfile.store.store.KeyValueStore;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class SynchronizedFileKeyValueStore<V> implements KeyValueStore<V> {

    private final FileProvider fileProvider;

    private final FileOperations<V> fileOperations;

    public SynchronizedFileKeyValueStore(FileProvider fileProvider,
                                         FileOperations<V> fileOperations) {
        this.fileProvider = fileProvider;
        this.fileOperations = fileOperations;
    }

    @Override
    public synchronized void init() {
        fileProvider.getOrCreateFile();
    }

    @Override
    public synchronized V save(String key, V value) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(value, "value cannot be null");
        validate(key, value);

        Map<String, V> all = new LinkedHashMap<>(getAll());
        all.put(key, value);
        Path filePath = fileProvider.getFilePath();
        fileOperations.write(filePath, all);
        return value;
    }

    @Override
    public synchronized V update(String key, Function<V, V> updateFunction) {
        V currentValue = getRequired(key).getValue();
        V newValue = updateFunction.apply(currentValue);
        return save(key, newValue);
    }

    @Override
    public synchronized V remove(String key) {
        Objects.requireNonNull(key, "key cannot be null");

        Map<String, V> all = new LinkedHashMap<>(getAll());
        V remove = all.remove(key);
        if (remove == null) {
            return null;
        }
        Path filePath = fileProvider.getFilePath();
        fileOperations.write(filePath, all);
        return remove;
    }

    @Override
    public synchronized void removeAll() {
        Path filePath = fileProvider.getFilePath();
        fileOperations.removeAll(filePath);
    }

    @Override
    public synchronized Map<String, V> getAll() {
        Path filePath = fileProvider.getFilePath();
        return fileOperations.read(filePath);
    }
}

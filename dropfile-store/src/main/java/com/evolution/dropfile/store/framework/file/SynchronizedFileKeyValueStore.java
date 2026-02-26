package com.evolution.dropfile.store.framework.file;

import com.evolution.dropfile.store.framework.KeyValueStore;

import java.nio.file.Path;
import java.util.*;
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
    public synchronized Collection<V> save(Map<String, V> values) {
        if (values.isEmpty()) {
            return Collections.emptyList();
        }

        for (Map.Entry<String, V> entry : values.entrySet()) {
            Objects.requireNonNull(entry.getKey(), "key cannot be null");
            Objects.requireNonNull(entry.getValue(), "value cannot be null");
        }

        Map<String, V> currentValues = Collections.unmodifiableMap(getAll());
        for (Map.Entry<String, V> entry : values.entrySet()) {
            validate(entry.getKey(), entry.getValue());
        }

        Map<String, V> newMap = new LinkedHashMap<>(currentValues);
        newMap.putAll(values);
        Path filePath = fileProvider.getFilePath();
        fileOperations.write(filePath, newMap);
        return List.copyOf(values.values());
    }

    @Override
    public synchronized V update(String key, Function<V, V> updateFunction) {
        V currentValue = getRequired(key).getValue();
        V newValue = updateFunction.apply(currentValue);
        validate(key, newValue);
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

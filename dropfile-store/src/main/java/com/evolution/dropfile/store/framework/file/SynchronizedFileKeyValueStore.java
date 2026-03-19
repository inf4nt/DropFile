package com.evolution.dropfile.store.framework.file;

import com.evolution.dropfile.store.framework.KeyValueStore;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

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
    public synchronized Collection<V> save(Supplier<? extends Map<String, V>> supplier, ValidatePolicy validatePolicy) {
        Objects.requireNonNull(supplier);
        Objects.requireNonNull(validatePolicy);

        Map<String, V> newValues = supplier.get();

        if (newValues == null || newValues.isEmpty()) {
            return Collections.emptyList();
        }

        for (Map.Entry<String, V> entry : newValues.entrySet()) {
            Objects.requireNonNull(entry.getKey(), "key cannot be null");
            Objects.requireNonNull(entry.getValue(), "value cannot be null");
        }

        Map<String, V> toSave = new LinkedHashMap<>();
        for (Map.Entry<String, V> entry : newValues.entrySet()) {
            try {
                validate(entry.getKey(), entry.getValue());
                toSave.put(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                if (validatePolicy == ValidatePolicy.GENTLE) {
                    continue;
                } else if (validatePolicy == ValidatePolicy.STRICT) {
                    throw e;
                }
                throw new IllegalArgumentException("Unknown validate policy " + validatePolicy);
            }
        }

        if (toSave.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, V> all = new LinkedHashMap<>(getAll());
        all.putAll(toSave);

        Path filePath = fileProvider.getFilePath();
        fileOperations.write(filePath, all);

        return Collections.unmodifiableCollection(toSave.values());
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

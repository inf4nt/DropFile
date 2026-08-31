package com.evolution.dropfile.store.framework.file;

import com.evolution.dropfile.store.framework.KeyValueStore;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class FileKeyValueStore<V> implements KeyValueStore<V> {

    private final FileProvider fileProvider;

    private final FileOperations fileOperations;

    private final SerdeOperations<V> serdeOperations;

    @SneakyThrows
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
        fileOperations.write(filePath, outputStream -> {
            serdeOperations.serialize(all, outputStream);
        });

        return Collections.unmodifiableCollection(toSave.values());
    }

    @SneakyThrows
    @Override
    public synchronized Collection<V> remove(Iterable<String> keys) {
        if (keys == null || !keys.iterator().hasNext()) {
            return Collections.emptyList();
        }

        Map<String, V> all = getAll();

        boolean hasMatches = false;
        for (String key : keys) {
            Objects.requireNonNull(key, "key cannot be null");
            if (!hasMatches && all.containsKey(key)) {
                hasMatches = true;
            }
        }

        if (!hasMatches) {
            return Collections.emptyList();
        }

        Map<String, V> toUpdate = new LinkedHashMap<>(all);
        List<V> removed = new ArrayList<>();
        for (String key : keys) {
            V value = toUpdate.remove(key);
            if (value != null) {
                removed.add(value);
            }
        }

        Path filePath = fileProvider.getFilePath();
        fileOperations.write(filePath, outputStream -> {
            serdeOperations.serialize(toUpdate, outputStream);
        });

        return removed;
    }

    @SneakyThrows
    @Override
    public synchronized void removeAll() {
        Path filePath = fileProvider.getFilePath();
        fileOperations.removeAll(filePath);
    }

    @SneakyThrows
    @Override
    public synchronized Map<String, V> getAll() {
        Path filePath = fileProvider.getFilePath();
        try (InputStream inputStream = fileOperations.read(filePath)) {
            return serdeOperations.deserialize(inputStream);
        } catch (NoContentFoundException e) {
            return Collections.emptyMap();
        }
    }
}

package com.evolution.dropfile.store.framework.file;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.CriteriaEnvelope;
import com.evolution.dropfile.store.framework.KeyValueStore;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class FileKeyValueStore<V> implements KeyValueStore<V> {

    private final FileProvider fileProvider;

    private final FileOperations fileOperations;

    private final SerdeOperations<V> serdeOperations;

    @SneakyThrows
    @Override
    public synchronized Map<String, V> save(Callable<? extends Map<String, V>> callable, ValidatePolicy validatePolicy) {
        Objects.requireNonNull(callable);
        Objects.requireNonNull(validatePolicy);

        Map<String, V> newValues = callable.call();

        if (newValues == null || newValues.isEmpty()) {
            return Collections.emptyMap();
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
            return Collections.emptyMap();
        }

        Map<String, V> all = new LinkedHashMap<>(getAll());
        all.putAll(toSave);

        Path filePath = fileProvider.getFilePath();
        fileOperations.write(filePath, outputStream -> {
            serdeOperations.serialize(all, outputStream);
        });

        return Collections.unmodifiableMap(toSave);
    }

    @Override
    public synchronized RemoveResult removeByCriteria(Collection<CriteriaEnvelope> criteriaEnvelopes) {
        if (criteriaEnvelopes == null || criteriaEnvelopes.isEmpty()) {
            return RemoveResult.EMPTY;
        }

        CommonUtils.MatchResult<Map.Entry<String, V>> matchResult = CommonUtils.matchBy(
                getAll().entrySet(),
                criteriaEnvelopes,
                (criteria, entry) -> entry.getKey().startsWith(criteria.value())
        );

        Map<CriteriaEnvelope, List<String>> ambiguous = matchResult.ambiguous().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream().map(Map.Entry::getKey).toList(),
                        (_, newVal) -> newVal,
                        LinkedHashMap::new
                ));

        if (matchResult.found().isEmpty()) {
            return new RemoveResult(
                    Collections.emptyMap(),
                    matchResult.notFound(),
                    ambiguous
            );
        }

        Set<String> keys = matchResult.found().values().stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        Map<String, V> removedByFullKey = remove(keys);

        Map<CriteriaEnvelope, String> confirmedFound = new LinkedHashMap<>();
        matchResult.found().forEach((criteria, entry) -> {
            V removedValue = removedByFullKey.get(entry.getKey());
            if (removedValue != null) {
                confirmedFound.put(criteria, entry.getKey());
            }
        });

        return new RemoveResult(
                confirmedFound,
                matchResult.notFound(),
                ambiguous
        );
    }

    @SneakyThrows
    @Override
    public synchronized Map<String, V> remove(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, V> all = getAll();
        if (all.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, V> toUpdate = new LinkedHashMap<>(all);
        Map<String, V> removed = new LinkedHashMap<>();

        for (String key : keys) {
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("Key must not be empty string");
            }
            V value = toUpdate.remove(key);
            if (value != null) {
                removed.put(key, value);
            }
        }

        if (!removed.isEmpty()) {
            Path filePath = fileProvider.getFilePath();
            fileOperations.write(filePath, outputStream -> {
                serdeOperations.serialize(toUpdate, outputStream);
            });
        }

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

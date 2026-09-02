package com.evolution.dropfile.store.framework;

import com.evolution.dropfile.common.CommonUtils;
import lombok.SneakyThrows;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public class RuntimeKeyValueStore<V> implements KeyValueStore<V> {

    private final Map<String, V> store = new LinkedHashMap<>();

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
            if (entry.getKey().isBlank()) {
                throw new IllegalArgumentException("Key must not be empty string");
            }
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

        store.putAll(toSave);

        return Collections.unmodifiableMap(toSave);
    }

    @Override
    public synchronized RemoveResult removeByCriteria(Collection<String> idCriteria) {
        if (idCriteria == null || idCriteria.isEmpty()) {
            return RemoveResult.EMPTY;
        }

        for (String criterion : idCriteria) {
            if (criterion == null || criterion.isBlank()) {
                throw new IllegalArgumentException("Id criterion must not be empty string");
            }
        }

        CommonUtils.MatchResult<String, Map.Entry<String, V>> matchResult = CommonUtils.matchBy(
                store.entrySet(),
                idCriteria,
                (criteria, entry) -> entry.getKey().startsWith(criteria)
        );

        Map<String, List<String>> ambiguous = matchResult.ambiguous().entrySet().stream()
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

        Set<String> fullKeys = matchResult.found().values().stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        Map<String, V> removedByFullKey = remove(fullKeys);

        Map<String, String> confirmedFound = new LinkedHashMap<>();
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

    @Override
    public synchronized Map<String, V> remove(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyMap();
        }

        for (String key : keys) {
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("Key must not be empty string");
            }
        }

        Map<String, V> removed = new LinkedHashMap<>();
        for (String key : keys) {
            V value = store.remove(key);
            if (value != null) {
                removed.put(key, value);
            }
        }

        return removed;
    }

    @Override
    public synchronized void removeAll() {
        store.clear();
    }

    @Override
    public synchronized Map<String, V> getAll() {
        return Collections.unmodifiableMap(store);
    }
}

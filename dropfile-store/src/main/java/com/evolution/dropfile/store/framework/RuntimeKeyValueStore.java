package com.evolution.dropfile.store.framework;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.CriteriaEnvelope;
import lombok.SneakyThrows;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class RuntimeKeyValueStore<V> implements KeyValueStore<V> {

    private static final long READ_LOCK_TIMEOUT_SECONDS = 60;

    private static final long WRITE_LOCK_TIMEOUT_SECONDS = 120;

    private final Map<String, V> store = new LinkedHashMap<>();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final Lock readLock = lock.readLock();

    private final Lock writeLock = lock.writeLock();

    private void acquireWriteLock() throws TimeoutException, InterruptedException {
        if (!writeLock.tryLock(WRITE_LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            throw new TimeoutException("Could not acquire write lock for %s within %d seconds"
                    .formatted(getClass().getSimpleName(), WRITE_LOCK_TIMEOUT_SECONDS));
        }
    }

    private void acquireReadLock() throws TimeoutException, InterruptedException {
        if (!readLock.tryLock(READ_LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            throw new TimeoutException("Could not acquire read lock for %s within %d seconds"
                    .formatted(getClass().getSimpleName(), READ_LOCK_TIMEOUT_SECONDS));
        }
    }

    @SneakyThrows
    @Override
    public Map<String, V> save(Callable<? extends Map<String, V>> callable, ValidatePolicy validatePolicy) {
        Objects.requireNonNull(callable);
        Objects.requireNonNull(validatePolicy);

        acquireWriteLock();
        try {
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
        } finally {
            writeLock.unlock();
        }
    }

    @SneakyThrows
    @Override
    public RemoveResult removeByCriteria(Collection<CriteriaEnvelope> criteriaEnvelopes) {
        if (criteriaEnvelopes == null || criteriaEnvelopes.isEmpty()) {
            return RemoveResult.EMPTY;
        }

        acquireWriteLock();
        try {
            CommonUtils.MatchResult<Map.Entry<String, V>> matchResult = CommonUtils.matchBy(
                    store.entrySet(),
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

            Set<String> fullKeys = matchResult.found().values().stream()
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());

            Map<String, V> removedByFullKey = remove(fullKeys);

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
        } finally {
            writeLock.unlock();
        }
    }

    @SneakyThrows
    @Override
    public Map<String, V> remove(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyMap();
        }

        acquireWriteLock();
        try {
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
        } finally {
            writeLock.unlock();
        }
    }

    @SneakyThrows
    @Override
    public void removeAll() {
        acquireWriteLock();
        try {
            store.clear();
        } finally {
            writeLock.unlock();
        }
    }

    @SneakyThrows
    @Override
    public Map<String, V> getAll() {
        acquireReadLock();
        try {
            return Collections.unmodifiableMap(new LinkedHashMap<>(store));
        } finally {
            readLock.unlock();
        }
    }
}
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class FileKeyValueStore<V> implements KeyValueStore<V> {

    private static final long READ_LOCK_TIMEOUT_SECONDS = 60;

    private static final long WRITE_LOCK_TIMEOUT_SECONDS = 120;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final Lock readLock = lock.readLock();

    private final Lock writeLock = lock.writeLock();

    private final FileProvider fileProvider;

    private final FileOperations fileOperations;

    private final SerdeOperations<V> serdeOperations;

    protected void doAfterMutation() {
    }

    @SneakyThrows
    @Override
    public Map<String, V> save(Callable<? extends Map<String, V>> callable,
                               UnaryOperator<Map<String, V>> preCommit,
                               ValidatePolicy validatePolicy) {
        Objects.requireNonNull(callable);
        Objects.requireNonNull(validatePolicy);

        acquireWriteLock();
        try {
            Map<String, V> newValues = callable.call();

            if (newValues == null || newValues.isEmpty()) {
                return Collections.emptyMap();
            }

            validateNotNull(newValues);

            newValues = Collections.unmodifiableMap(newValues);

            Map<String, V> toSave = validateEntries(newValues, validatePolicy);

            if (toSave.isEmpty()) {
                return Collections.emptyMap();
            }

            if (preCommit != null) {
                toSave = Collections.unmodifiableMap(toSave);

                toSave = preCommit.apply(toSave);

                if (toSave == null || toSave.isEmpty()) {
                    return Collections.emptyMap();
                }

                validateNotNull(toSave);
                toSave = validateEntries(toSave, validatePolicy);

                if (toSave.isEmpty()) {
                    return Collections.emptyMap();
                }
            }

            Map<String, V> all = new LinkedHashMap<>(getAll());
            all.putAll(toSave);

            Path filePath = fileProvider.getFilePath();
            fileOperations.write(filePath, outputStream -> {
                serdeOperations.serialize(all, outputStream);
            });

            afterMutation();

            return Collections.unmodifiableMap(toSave);
        } finally {
            writeLock.unlock();
        }
    }

    private void validateNotNull(Map<String, V> map) {
        for (Map.Entry<String, V> entry : map.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("key cannot be null or empty");
            }
            Objects.requireNonNull(entry.getValue(), "value cannot be null");
        }
    }

    private Map<String, V> validateEntries(Map<String, V> entries,
                                           ValidatePolicy validatePolicy) {
        Map<String, V> validEntries = new LinkedHashMap<>();
        for (Map.Entry<String, V> entry : entries.entrySet()) {
            try {
                validate(entry.getKey(), entry.getValue());
                validEntries.put(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                if (validatePolicy == ValidatePolicy.STRICT) {
                    throw e;
                } else if (validatePolicy != ValidatePolicy.GENTLE) {
                    throw new IllegalArgumentException("Unknown validate policy " + validatePolicy);
                }
            }
        }
        return validEntries;
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

            afterMutation();

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

            if (removed.isEmpty()) {
                return Collections.emptyMap();
            }

            Path filePath = fileProvider.getFilePath();
            fileOperations.write(filePath, outputStream -> {
                serdeOperations.serialize(toUpdate, outputStream);
            });

            afterMutation();

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
            Path filePath = fileProvider.getFilePath();
            fileOperations.removeAll(filePath);

            afterMutation();
        } finally {
            writeLock.unlock();
        }
    }

    @SneakyThrows
    @Override
    public Map<String, V> getAll() {
        acquireReadLock();
        try {
            Path filePath = fileProvider.getFilePath();
            try (InputStream inputStream = fileOperations.read(filePath)) {
                return serdeOperations.deserialize(inputStream);
            } catch (NoContentFoundException e) {
                return Collections.emptyMap();
            }
        } finally {
            readLock.unlock();
        }
    }

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

    private void afterMutation() {
        try {
            doAfterMutation();
        } catch (Exception _) {
            // nothing to do
        }
    }
}

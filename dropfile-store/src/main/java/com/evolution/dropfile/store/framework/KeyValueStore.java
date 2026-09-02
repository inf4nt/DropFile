package com.evolution.dropfile.store.framework;

import com.evolution.dropfile.common.CommonUtils;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Function;

public interface KeyValueStore<V> {

    Map<String, V> save(Callable<? extends Map<String, V>> callable, ValidatePolicy validatePolicy);

    default Map<String, V> save(Callable<? extends Map<String, V>> callable) {
        return save(callable, ValidatePolicy.STRICT);
    }

    default V save(String key, Callable<V> callable) {
        return save(() -> Map.of(key, callable.call()), ValidatePolicy.STRICT).values().stream().findAny().orElseThrow();
    }

    default V save(String key, V value) {
        return save(() -> Map.of(key, value), ValidatePolicy.STRICT).values().stream().findAny().orElseThrow();
    }

    default V update(String key, Function<V, V> updateFunction) {
        return save(
                () -> {
                    Map.Entry<String, V> current = getRequired(key);
                    V newValue = updateFunction.apply(current.getValue());
                    return Map.of(key, newValue);
                },
                ValidatePolicy.STRICT
        ).values().stream().findAny().orElseThrow();
    }

    RemoveResult removeByCriteria(Collection<String> idCriteria);

    Map<String, V> remove(Collection<String> keys);

    default V remove(String key) {
        return remove(Set.of(key)).entrySet()
                .stream()
                .findAny()
                .map(it -> it.getValue())
                .orElse(null);
    }

    void removeAll();

    Map<String, V> getAll();

    default void validate(String key, V value) {

    }

    default Optional<Map.Entry<String, V>> get(String key) {
        return Optional.ofNullable(getAll().get(key))
                .map(it -> Map.entry(key, it));
    }

    default Map.Entry<String, V> getRequired(String key) {
        return get(key)
                .orElseThrow(() -> new NoSuchElementException(String.format(
                        "Store %s. No key %s found", getClass().getSimpleName(), key
                )));
    }

    default Map.Entry<String, V> getRequiredByKeyStartWith(String stringKey) {
        CommonUtils.MatchResult<String, Map.Entry<String, V>> matchResult = CommonUtils.matchBy(
                getAll().entrySet(),
                List.of(stringKey),
                (criteria, entry) -> entry.getKey().startsWith(criteria)
        );

        if (!matchResult.notFound().isEmpty()) {
            throw new NoSuchElementException(
                    "Store %s. No items found for criteria: %s".formatted(getClass().getSimpleName(), stringKey)
            );
        }

        if (matchResult.found().containsKey(stringKey) && matchResult.ambiguous().isEmpty()) {
            return matchResult.found().get(stringKey);
        }

        List<Map.Entry<String, V>> matches = matchResult.ambiguous().get(stringKey);
        int matchesCount = (matches != null) ? matches.size() : 0;

        throw new IllegalStateException(
                "Store %s. Ambiguous key criteria '%s'. Found %d matches".formatted(
                        getClass().getSimpleName(), stringKey, matchesCount
                )
        );
    }

    enum ValidatePolicy {
        STRICT,
        GENTLE
    }

    record RemoveResult(
            Map<String, String> removed,
            Collection<String> notFound,
            Map<String, List<String>> ambiguous
    ) {
        public static final RemoveResult EMPTY = new RemoveResult(Map.of(), Set.of(), Map.of());
    }
}

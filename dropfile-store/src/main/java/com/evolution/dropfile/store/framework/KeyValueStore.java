package com.evolution.dropfile.store.framework;

import com.evolution.dropfile.common.CommonUtils;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public interface KeyValueStore<V> {

    Collection<V> save(Supplier<? extends Map<String, V>> supplier, ValidatePolicy validatePolicy);

    default Collection<V> save(Supplier<? extends Map<String, V>> supplier) {
        return save(supplier, ValidatePolicy.STRICT);
    }

    default V save(String key, Supplier<V> valueSupplier) {
        return save(() -> Map.of(key, valueSupplier.get())).iterator().next();
    }

    default V save(String key, V value) {
        return save(() -> Map.of(key, value)).iterator().next();
    }

    default V update(String key, Function<V, V> updateFunction) {
        return save(
                () -> {
                    Map.Entry<String, V> current = getRequired(key);
                    V newValue = updateFunction.apply(current.getValue());
                    return Map.of(key, newValue);
                },
                ValidatePolicy.STRICT
        ).iterator().next();
    }

    Collection<V> remove(Set<String> keys);

    default V remove(String key) {
        Collection<V> remove = remove(Set.of(key));
        return remove.isEmpty() ? null : remove.iterator().next();
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
                        "Store %s. No key %s found", getClass().getName(), key
                )));
    }

    default Map<String, V> getByKeyStartWith(Set<String> stringKeys) {
        if (stringKeys == null || stringKeys.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, V> all = getAll();
        Map<String, V> found = new LinkedHashMap<>();

        for (String stringKey : stringKeys) {
            Objects.requireNonNull(stringKey, "search key prefix cannot be null");

            Map.Entry<String, V> one = CommonUtils.one(
                    all.entrySet(),
                    entry -> entry.getKey().startsWith(stringKey),
                    () -> String.format("Store %s (prefix: '%s')", getClass().getName(), stringKey)
            ).orElse(null);

            if (one != null) {
                if (found.containsKey(one.getKey())) {
                    throw new IllegalStateException(
                            "Store %s. Unable to complete 'getByKeyStartWith'. Duplicate key '%s' matched for prefix '%s'"
                                    .formatted(getClass().getName(), one.getKey(), stringKey)
                    );
                }
                found.put(one.getKey(), one.getValue());
            }
        }

        return found;
    }

    default Map.Entry<String, V> getRequiredByKeyStartWith(String stringKey) {
        return CommonUtils.requireOne(
                getAll().entrySet(),
                entry -> entry.getKey().startsWith(stringKey),
                () -> String.format("Store %s", getClass().getName())
        );
    }

    enum ValidatePolicy {
        STRICT,
        GENTLE
    }
}

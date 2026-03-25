package com.evolution.dropfile.store.framework;

import com.evolution.dropfile.common.CommonUtils;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public interface KeyValueStore<V> {

    default Map.Entry<String, V> getRequired(String key,
                                             Predicate<V> predicate) {
        Map.Entry<String, V> required = getRequired(key);
        boolean test = predicate.test(required.getValue());
        if (!test) {
            throw new RuntimeException(String.format(
                    "Store %s. Filter predicate failed %s", getClass().getName(), key
            ));
        }
        return required;
    }

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
                    Objects.requireNonNull(newValue);
                    validate(key, newValue);
                    return Map.of(key, newValue);
                },
                ValidatePolicy.STRICT
        ).iterator().next();
    }

    V remove(String key);

    void removeAll();

    Map<String, V> getAll();

    void init();

    default void validate(String key, V value) {

    }

    default Optional<Map.Entry<String, V>> get(String key) {
        return Optional.ofNullable(getAll().get(key))
                .map(it -> new AbstractMap.SimpleEntry<>(key, it));
    }

    default Map.Entry<String, V> getRequired(String key) {
        return get(key)
                .orElseThrow(() -> new RuntimeException(String.format(
                        "Store %s. No key %s found", getClass().getName(), key
                )));
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

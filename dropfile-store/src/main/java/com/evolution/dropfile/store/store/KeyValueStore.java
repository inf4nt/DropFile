package com.evolution.dropfile.store.store;

import java.util.*;
import java.util.function.Function;

public interface KeyValueStore<V> {

    Collection<V> save(Map<String, V> values);

    default V save(String key, V value) {
        return save(Map.of(key, value)).iterator().next();
    }

    V update(String key, Function<V, V> updateFunction);

    V remove(String key);

    void removeAll();

    Map<String, V> getAll();

    default void init() {

    }

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
        List<Map.Entry<String, V>> list = getAll().entrySet().stream()
                .filter(it -> it.getKey().startsWith(stringKey)).toList();
        if (list.isEmpty()) {
            throw new RuntimeException(String.format(
                    "Store %s. No found by criteria(key.startWith(%s))", getClass().getName(), stringKey
            ));
        }
        if (list.size() != 1) {
            throw new RuntimeException(String.format("More than one item was found. Please provide more detailed criteria. Found: %s items", list.size()));
        }
        return list.getFirst();
    }
}

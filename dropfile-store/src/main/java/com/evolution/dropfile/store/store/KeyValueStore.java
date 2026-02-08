package com.evolution.dropfile.store.store;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface KeyValueStore<V> {

    V save(String key, V value);

    V remove(String key);

    void removeAll();

    Map<String, V> getAll();

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

    default V removeByKeyStartWith(String string) {
        List<Map.Entry<String, V>> list = getAll().entrySet().stream()
                .filter(it -> it.getKey().startsWith(string)).toList();
        if (list.isEmpty()) {
            return null;
        }
        if (list.size() != 1) {
            throw new RuntimeException(String.format("More than one item was found for removal. Please provide more detailed criteria. Found: %s", list.size()));
        }
        Map.Entry<String, V> first = list.getFirst();
        return remove(first.getKey());
    }
}

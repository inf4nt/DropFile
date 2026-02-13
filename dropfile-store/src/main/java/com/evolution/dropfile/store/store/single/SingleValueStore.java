package com.evolution.dropfile.store.store.single;

import java.util.Optional;

public interface SingleValueStore<V> {

    Optional<V> get();

    default V getRequired() {
        return get().orElseThrow();
    }

    V save(V value);

    V remove();

    default void init() {

    }

    default void validate(V value) {

    }
}

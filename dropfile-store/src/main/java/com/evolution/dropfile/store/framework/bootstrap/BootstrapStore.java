package com.evolution.dropfile.store.framework.bootstrap;

import java.util.Optional;

public interface BootstrapStore<V> {

    default V getRequired() {
        return get().orElseThrow();
    }

    Optional<V> get();

    V save(V value);

    V remove();

    default void validate(V value) {

    }
}

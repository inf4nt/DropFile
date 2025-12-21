package com.evolution.dropfile.configuration.store.single;

import java.util.Objects;
import java.util.Optional;

public class ImmutableSingleValueStore<V>
        implements SingleValueStore<V> {

    private final V value;

    public ImmutableSingleValueStore(V value) {
        this.value = Objects.requireNonNull(value);
    }

    @Override
    public Optional<V> get() {
        return Optional.of(value);
    }

    @Override
    public V save(V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V remove() {
        throw new UnsupportedOperationException();
    }
}

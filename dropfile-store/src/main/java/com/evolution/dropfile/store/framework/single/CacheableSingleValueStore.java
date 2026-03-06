package com.evolution.dropfile.store.framework.single;

import com.evolution.dropfile.store.framework.Cacheable;

import java.util.Optional;

public class CacheableSingleValueStore<V>
        implements SingleValueStore<V>, Cacheable {

    volatile private Optional<V> cache;

    private final SingleValueStore<V> delegate;

    public CacheableSingleValueStore(SingleValueStore<V> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Optional<V> get() {
        Optional<V> result = cache;

        if (result == null) {
            synchronized (this) {
                result = cache;
                if (result == null) {
                    result = delegate.get();
                    cache = result;
                }
            }
        }
        return result;
    }

    @Override
    public synchronized V save(V value) {
        try {
            return delegate.save(value);
        } finally {
            reset();
        }
    }

    @Override
    public synchronized V remove() {
        try {
            return delegate.remove();
        } finally {
            reset();
        }
    }

    @Override
    public synchronized void reset() {
        cache = null;
    }

    @Override
    public void init() {
        delegate.init();
    }
}

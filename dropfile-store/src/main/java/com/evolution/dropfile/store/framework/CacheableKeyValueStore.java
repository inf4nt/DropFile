package com.evolution.dropfile.store.framework;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class CacheableKeyValueStore<V> implements KeyValueStore<V>, Cacheable {

    volatile private Map<String, V> cache;

    private final KeyValueStore<V> delegate;

    public CacheableKeyValueStore(KeyValueStore<V> delegate) {
        this.delegate = delegate;
    }

    @Override
    public synchronized void reset() {
        cache = null;
    }

    @Override
    public synchronized Collection<V> save(Supplier<? extends Map<String, V>> valuesSupplier) {
        try {
            return delegate.save(valuesSupplier);
        } finally {
            reset();
        }
    }

    @Override
    public synchronized V update(String key, Function<V, V> updateFunction) {
        try {
            return delegate.update(key, updateFunction);
        } finally {
            reset();
        }
    }

    @Override
    public synchronized V remove(String key) {
        try {
            return delegate.remove(key);
        } finally {
            reset();
        }
    }

    @Override
    public synchronized void removeAll() {
        try {
            delegate.removeAll();
        } finally {
            reset();
        }
    }

    @Override
    public Map<String, V> getAll() {
        Map<String, V> result = cache;

        if (result == null) {
            synchronized (this) {
                result = cache;
                if (result == null) {
                    Map<String, V> all = delegate.getAll();
                    result = Collections.unmodifiableMap(all);
                    cache = result;
                }
            }
        }
        return result;
    }
}

package com.evolution.dropfile.store.framework.file;

import com.evolution.dropfile.store.framework.CacheableKeyValueStore;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class CacheableSynchronizedFileKeyValueStore<V>
        extends SynchronizedFileKeyValueStore<V>
        implements CacheableKeyValueStore<V> {

    volatile Map<String, V> cache;

    public CacheableSynchronizedFileKeyValueStore(FileProvider fileProvider, FileOperations<V> fileOperations) {
        super(fileProvider, fileOperations);
    }

    @Override
    public synchronized void init() {
        super.init();
    }

    @Override
    public synchronized Collection<V> save(Supplier<? extends Map<String, V>> supplier, ValidatePolicy validatePolicy) {
        try {
            return super.save(supplier, validatePolicy);
        } finally {
            reset();
        }
    }

    @Override
    public synchronized V update(String key, Function<V, V> updateFunction) {
        try {
            return super.update(key, updateFunction);
        } finally {
            reset();
        }
    }

    @Override
    public synchronized V remove(String key) {
        try {
            return super.remove(key);
        } finally {
            reset();
        }
    }

    @Override
    public synchronized void removeAll() {
        try {
            super.removeAll();
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
                    Map<String, V> all = super.getAll();
                    result = Collections.unmodifiableMap(all);
                    cache = result;
                }
            }
        }
        return result;
    }

    @Override
    public synchronized void reset() {
        cache = null;
    }
}

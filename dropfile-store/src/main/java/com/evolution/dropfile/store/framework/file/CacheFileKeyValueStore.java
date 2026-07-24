package com.evolution.dropfile.store.framework.file;

import com.evolution.dropfile.store.framework.CacheableKeyValueStore;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

public class CacheFileKeyValueStore<V>
        extends FileKeyValueStore<V>
        implements CacheableKeyValueStore<V> {

    volatile private Map<String, V> cache;

    public CacheFileKeyValueStore(FileProvider fileProvider,
                                  FileOperations fileOperations,
                                  SerdeOperations<V> serdeOperations) {
        super(fileProvider, fileOperations, serdeOperations);
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
    public synchronized V putIfAbsent(String key, V value) {
        try {
            return super.putIfAbsent(key, value);
        } finally {
            reset();
        }
    }

    @Override
    public synchronized void reset() {
        cache = null;
    }
}

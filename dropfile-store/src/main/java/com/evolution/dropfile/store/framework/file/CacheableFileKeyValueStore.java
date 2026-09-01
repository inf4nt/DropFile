package com.evolution.dropfile.store.framework.file;

import com.evolution.dropfile.store.framework.CacheableKeyValueStore;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

public class CacheableFileKeyValueStore<V>
        extends FileKeyValueStore<V>
        implements CacheableKeyValueStore<V> {

    volatile private Map<String, V> cache;

    public CacheableFileKeyValueStore(FileProvider fileProvider,
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
    public synchronized Map<String, V> remove(Collection<String> keys) {
        try {
            return super.remove(keys);
        } finally {
            reset();
        }
    }

    @Override
    public synchronized RemoveResult<V> removeByCriteria(Collection<String> idCriteria) {
        try {
            return super.removeByCriteria(idCriteria);
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

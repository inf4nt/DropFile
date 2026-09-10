package com.evolution.dropfile.store.framework.file;

import com.evolution.dropfile.store.framework.CacheableKeyValueStore;

import java.util.Collections;
import java.util.Map;

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
    protected void afterMutation() {
        reset();
    }

    @Override
    public void reset() {
        cache = null;
    }
}

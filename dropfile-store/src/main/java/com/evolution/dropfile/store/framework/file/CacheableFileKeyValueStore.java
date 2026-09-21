package com.evolution.dropfile.store.framework.file;

import com.evolution.dropfile.store.framework.CacheableKeyValueStore;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeoutException;

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
            try {
                acquireReadLock();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e.getMessage(), e);
            } catch (TimeoutException e) {
                throw new RuntimeException(e.getMessage(), e);
            }

            try {
                result = cache;
                if (result == null) {
                    Map<String, V> all = doGetAll();
                    result = Collections.unmodifiableMap(all);
                    cache = result;
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e.getMessage(), e);
            } finally {
                readLock.unlock();
            }
        }
        return result;
    }

    @Override
    protected void doAfterMutation() {
        reset();
    }

    @Override
    public void reset() {
        cache = null;
    }
}

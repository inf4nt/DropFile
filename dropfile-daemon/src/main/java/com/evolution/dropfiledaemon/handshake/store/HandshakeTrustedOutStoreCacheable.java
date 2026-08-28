package com.evolution.dropfiledaemon.handshake.store;

import com.evolution.dropfile.store.framework.file.CacheableFileKeyValueStore;
import com.evolution.dropfile.store.framework.file.FileOperations;
import com.evolution.dropfile.store.framework.file.FileProvider;
import com.evolution.dropfile.store.framework.file.SerdeOperations;

public class HandshakeTrustedOutStoreCacheable
        extends CacheableFileKeyValueStore<HandshakeTrustedOutStore.TrustedOut>
        implements HandshakeTrustedOutStore {

    public HandshakeTrustedOutStoreCacheable(FileProvider fileProvider, FileOperations fileOperations, SerdeOperations<TrustedOut> serdeOperations) {
        super(fileProvider, fileOperations, serdeOperations);
    }
}

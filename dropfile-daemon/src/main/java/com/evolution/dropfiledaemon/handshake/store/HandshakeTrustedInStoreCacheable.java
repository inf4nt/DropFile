package com.evolution.dropfiledaemon.handshake.store;

import com.evolution.dropfile.store.framework.file.CacheableFileKeyValueStore;
import com.evolution.dropfile.store.framework.file.FileOperations;
import com.evolution.dropfile.store.framework.file.FileProvider;
import com.evolution.dropfile.store.framework.file.SerdeOperations;

public class HandshakeTrustedInStoreCacheable
        extends CacheableFileKeyValueStore<HandshakeTrustedInStore.TrustedIn>
        implements HandshakeTrustedInStore {

    public HandshakeTrustedInStoreCacheable(FileProvider fileProvider, FileOperations fileOperations, SerdeOperations<TrustedIn> serdeOperations) {
        super(fileProvider, fileOperations, serdeOperations);
    }
}

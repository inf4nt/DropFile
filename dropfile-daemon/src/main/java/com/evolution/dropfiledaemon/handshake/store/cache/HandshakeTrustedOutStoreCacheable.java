package com.evolution.dropfiledaemon.handshake.store.cache;

import com.evolution.dropfile.store.framework.file.CacheableFileKeyValueStore;
import com.evolution.dropfile.store.framework.file.FileOperations;
import com.evolution.dropfile.store.framework.file.FileProvider;
import com.evolution.dropfile.store.framework.file.SerdeOperations;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedOutStore;

public class HandshakeTrustedOutStoreCacheable
        extends CacheableFileKeyValueStore<HandshakeTrustedOutStore.TrustedOut>
        implements HandshakeTrustedOutStore {

    public HandshakeTrustedOutStoreCacheable(FileProvider fileProvider, FileOperations fileOperations, SerdeOperations<TrustedOut> serdeOperations) {
        super(fileProvider, fileOperations, serdeOperations);
    }
}

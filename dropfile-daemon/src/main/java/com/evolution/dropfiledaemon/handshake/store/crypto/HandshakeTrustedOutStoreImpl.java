package com.evolution.dropfiledaemon.handshake.store.crypto;

import com.evolution.dropfile.store.framework.file.CacheFileKeyValueStore;
import com.evolution.dropfile.store.framework.file.FileOperations;
import com.evolution.dropfile.store.framework.file.FileProvider;
import com.evolution.dropfile.store.framework.file.SerdeOperations;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedOutStore;

public class HandshakeTrustedOutStoreImpl
        extends CacheFileKeyValueStore<HandshakeTrustedOutStore.TrustedOut>
        implements HandshakeTrustedOutStore {

    public HandshakeTrustedOutStoreImpl(FileProvider fileProvider, FileOperations fileOperations, SerdeOperations<TrustedOut> serdeOperations) {
        super(fileProvider, fileOperations, serdeOperations);
    }
}

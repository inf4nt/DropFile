package com.evolution.dropfiledaemon.handshake.store.crypto;

import com.evolution.dropfile.store.framework.file.CacheFileKeyValueStore;
import com.evolution.dropfile.store.framework.file.FileOperations;
import com.evolution.dropfile.store.framework.file.FileProvider;
import com.evolution.dropfile.store.framework.file.SerdeOperations;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedInStore;

public class HandshakeTrustedInStoreImpl
        extends CacheFileKeyValueStore<HandshakeTrustedInStore.TrustedIn>
        implements HandshakeTrustedInStore {

    public HandshakeTrustedInStoreImpl(FileProvider fileProvider, FileOperations fileOperations, SerdeOperations<TrustedIn> serdeOperations) {
        super(fileProvider, fileOperations, serdeOperations);
    }
}

package com.evolution.dropfiledaemon.handshake.store.crypto;

import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.store.store.file.CryptoFileOperations;
import com.evolution.dropfile.store.store.file.FileProvider;
import com.evolution.dropfile.store.store.file.FileProviderImpl;
import com.evolution.dropfile.store.store.file.SynchronizedFileKeyValueStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedOutStore;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CryptoHandshakeTrustedOutStore
        extends SynchronizedFileKeyValueStore<HandshakeTrustedOutStore.TrustedOut>
        implements HandshakeTrustedOutStore {

    public CryptoHandshakeTrustedOutStore(ObjectMapper objectMapper, CryptoTunnel cryptoTunnel) {
        FileProvider fileProvider = new FileProviderImpl("trustout.bin");
        super(
                fileProvider,
                new CryptoFileOperations<>(
                        objectMapper,
                        TrustedOut.class,
                        fileProvider,
                        cryptoTunnel
                )
        );
    }
}

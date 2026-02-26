package com.evolution.dropfiledaemon.handshake.store.crypto;

import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.store.framework.file.CryptoFileOperations;
import com.evolution.dropfile.store.framework.file.FileProvider;
import com.evolution.dropfile.store.framework.file.FileProviderImpl;
import com.evolution.dropfile.store.framework.file.SynchronizedFileKeyValueStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedInStore;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CryptoHandshakeTrustedInStore
        extends SynchronizedFileKeyValueStore<HandshakeTrustedInStore.TrustedIn>
        implements HandshakeTrustedInStore {

    public CryptoHandshakeTrustedInStore(ObjectMapper objectMapper, CryptoTunnel cryptoTunnel) {
        FileProvider fileProvider = new FileProviderImpl("trustin.bin");
        super(
                fileProvider,
                new CryptoFileOperations<>(
                        objectMapper,
                        TrustedIn.class,
                        fileProvider,
                        cryptoTunnel
                )
        );
    }
}

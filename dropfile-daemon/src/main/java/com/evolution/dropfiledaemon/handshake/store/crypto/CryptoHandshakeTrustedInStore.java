package com.evolution.dropfiledaemon.handshake.store.crypto;

import com.evolution.dropfile.common.FileHelper;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.store.framework.file.*;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedInStore;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;

public class CryptoHandshakeTrustedInStore
        extends CacheableSynchronizedFileKeyValueStore<HandshakeTrustedInStore.TrustedIn>
        implements HandshakeTrustedInStore {

    public CryptoHandshakeTrustedInStore(FileHelper fileHelper, ObjectMapper objectMapper, CryptoTunnel cryptoTunnel, Path parrentDirectoryPath) {
        FileProvider fileProvider = new FileProviderImpl(parrentDirectoryPath, "trustin.bin");
        super(
                fileProvider,
                new CryptoFileOperations<>(
                        fileHelper,
                        objectMapper,
                        TrustedIn.class,
                        fileProvider,
                        cryptoTunnel
                )
        );
    }
}

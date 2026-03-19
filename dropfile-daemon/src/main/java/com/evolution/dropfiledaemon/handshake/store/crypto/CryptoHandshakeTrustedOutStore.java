package com.evolution.dropfiledaemon.handshake.store.crypto;

import com.evolution.dropfile.common.FileHelper;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.store.framework.file.CacheableSynchronizedFileKeyValueStore;
import com.evolution.dropfile.store.framework.file.CryptoFileOperations;
import com.evolution.dropfile.store.framework.file.FileProvider;
import com.evolution.dropfile.store.framework.file.FileProviderImpl;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedOutStore;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;

public class CryptoHandshakeTrustedOutStore
        extends CacheableSynchronizedFileKeyValueStore<HandshakeTrustedOutStore.TrustedOut>
        implements HandshakeTrustedOutStore {

    public CryptoHandshakeTrustedOutStore(FileHelper fileHelper, ObjectMapper objectMapper, CryptoTunnel cryptoTunnel, Path parrentDirectoryPath) {
        FileProvider fileProvider = new FileProviderImpl(parrentDirectoryPath, "trustout.bin");
        super(
                fileProvider,
                new CryptoFileOperations<>(
                        fileHelper,
                        objectMapper,
                        TrustedOut.class,
                        fileProvider,
                        cryptoTunnel
                )
        );
    }
}

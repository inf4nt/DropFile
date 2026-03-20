package com.evolution.dropfiledaemon.handshake.store.crypto;

import com.evolution.dropfile.common.FileHelper;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.store.framework.file.ApplicationFingerprintSupplier;
import com.evolution.dropfile.store.framework.file.*;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedOutStore;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;

public class CryptoHandshakeTrustedOutStore
        extends CacheableSynchronizedFileKeyValueStore<HandshakeTrustedOutStore.TrustedOut>
        implements HandshakeTrustedOutStore {

    public CryptoHandshakeTrustedOutStore(FileHelper fileHelper,
                                          ObjectMapper objectMapper,
                                          CryptoTunnel cryptoTunnel,
                                          ApplicationFingerprintSupplier applicationFingerprintSupplier,
                                          Path applicationConfigDirectoryPath) {
        super(
                new FileProviderImpl(applicationConfigDirectoryPath, ".trustout.bin"),
                new CryptoFileOperations<>(
                        fileHelper,
                        objectMapper,
                        TrustedOut.class,
                        cryptoTunnel,
                        applicationFingerprintSupplier
                )
        );
    }
}

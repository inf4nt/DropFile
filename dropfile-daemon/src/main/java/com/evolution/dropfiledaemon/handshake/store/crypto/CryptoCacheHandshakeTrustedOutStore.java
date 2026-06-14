package com.evolution.dropfiledaemon.handshake.store.crypto;

import com.evolution.dropfile.common.FileHelper;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.store.framework.file.*;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedOutStore;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;

public class CryptoCacheHandshakeTrustedOutStore
        extends CacheFileKeyValueStore<HandshakeTrustedOutStore.TrustedOut>
        implements HandshakeTrustedOutStore {

    public CryptoCacheHandshakeTrustedOutStore(FileHelper fileHelper,
                                               ObjectMapper objectMapper,
                                               CryptoTunnel cryptoTunnel,
                                               InstallationSeedProvider installationSeedProvider,
                                               Path applicationConfigDirectoryPath) {
        super(
                new FileProviderImpl(
                        applicationConfigDirectoryPath,
                        ".trustout.bin"
                ),
                new CryptoFileOperationsDecorator(
                        new FileSystemOperations(
                                fileHelper
                        ),
                        cryptoTunnel,
                        installationSeedProvider
                ),
                new JsonSerdeOperations<>(objectMapper, TrustedOut.class)
        );
    }
}

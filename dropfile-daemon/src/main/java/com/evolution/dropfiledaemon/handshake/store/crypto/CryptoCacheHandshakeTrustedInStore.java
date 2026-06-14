package com.evolution.dropfiledaemon.handshake.store.crypto;

import com.evolution.dropfile.common.FileHelper;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.store.framework.file.*;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedInStore;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;

public class CryptoCacheHandshakeTrustedInStore
        extends CacheFileKeyValueStore<HandshakeTrustedInStore.TrustedIn>
        implements HandshakeTrustedInStore {

    public CryptoCacheHandshakeTrustedInStore(FileHelper fileHelper,
                                              ObjectMapper objectMapper,
                                              CryptoTunnel cryptoTunnel,
                                              InstallationSeedProvider installationSeedProvider,
                                              Path applicationConfigDirectoryPath) {
        super(
                new FileProviderImpl(
                        applicationConfigDirectoryPath,
                        ".trustin.bin"
                ),
                new CryptoFileOperationsDecorator(
                        new FileSystemOperations(
                                fileHelper
                        ),
                        cryptoTunnel,
                        installationSeedProvider
                ),
                new JsonSerdeOperations<>(objectMapper, TrustedIn.class)
        );
    }
}

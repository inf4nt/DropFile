package com.evolution.dropfile.store.secret;

import com.evolution.dropfile.common.FileHelper;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.store.framework.file.*;
import com.evolution.dropfile.store.framework.single.CacheableDefaultSingleValueStore;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;

public class CryptoCacheDaemonSecretsStore
        extends CacheableDefaultSingleValueStore<DaemonSecrets>
        implements DaemonSecretsStore {

    public CryptoCacheDaemonSecretsStore(FileHelper fileHelper,
                                         ObjectMapper objectMapper,
                                         CryptoTunnel cryptoTunnel,
                                         InstallationSeedProvider installationSeedProvider,
                                         Path applicationConfigDirectoryPath) {
        super(
                "daemonSecrets",
                new CacheFileKeyValueStore<>(
                        new FileProviderImpl(applicationConfigDirectoryPath, ".daemon.bin"),
                        new CryptoFileOperationsDecorator(
                                new FileSystemOperations(fileHelper),
                                cryptoTunnel,
                                installationSeedProvider
                        ),
                        new JsonSerdeOperations<>(objectMapper, DaemonSecrets.class)
                )
        );
    }
}

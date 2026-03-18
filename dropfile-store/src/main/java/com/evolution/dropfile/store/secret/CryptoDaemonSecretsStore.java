package com.evolution.dropfile.store.secret;

import com.evolution.dropfile.common.FileHelper;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.store.framework.file.CryptoFileOperations;
import com.evolution.dropfile.store.framework.file.FileProvider;
import com.evolution.dropfile.store.framework.file.FileProviderImpl;
import com.evolution.dropfile.store.framework.file.SynchronizedFileKeyValueStore;
import com.evolution.dropfile.store.framework.single.DefaultSingleValueStore;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;

public class CryptoDaemonSecretsStore
        extends DefaultSingleValueStore<DaemonSecrets>
        implements DaemonSecretsStore {

    public CryptoDaemonSecretsStore(FileHelper fileHelper,
                                    ObjectMapper objectMapper,
                                    CryptoTunnel cryptoTunnel,
                                    Path parrentDirectoryPath) {
        FileProvider fileProvider = new FileProviderImpl(parrentDirectoryPath, "daemon.secrets.bin");
        super(
                "daemonSecrets",
                new SynchronizedFileKeyValueStore<>(
                        fileProvider,
                        new CryptoFileOperations<>(
                                fileHelper,
                                objectMapper,
                                DaemonSecrets.class,
                                fileProvider,
                                cryptoTunnel
                        )
                )
        );
    }
}

package com.evolution.dropfile.store.secret;

import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.store.framework.file.CryptoFileOperations;
import com.evolution.dropfile.store.framework.file.FileProvider;
import com.evolution.dropfile.store.framework.file.FileProviderImpl;
import com.evolution.dropfile.store.framework.file.SynchronizedFileKeyValueStore;
import com.evolution.dropfile.store.framework.single.DefaultSingleValueStore;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CryptoDaemonSecretsStore
        extends DefaultSingleValueStore<DaemonSecrets>
        implements DaemonSecretsStore {

    public CryptoDaemonSecretsStore(ObjectMapper objectMapper,
                                    CryptoTunnel cryptoTunnel) {
        FileProvider fileProvider = new FileProviderImpl("daemon.secrets.bin");
        super(
                "daemonSecrets",
                new SynchronizedFileKeyValueStore<>(
                        fileProvider,
                        new CryptoFileOperations<>(
                                objectMapper,
                                DaemonSecrets.class,
                                fileProvider,
                                cryptoTunnel
                        )
                )
        );
    }
}

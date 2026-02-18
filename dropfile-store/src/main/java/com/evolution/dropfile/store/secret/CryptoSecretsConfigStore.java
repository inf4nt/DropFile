package com.evolution.dropfile.store.secret;

import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.store.store.file.CryptoFileOperations;
import com.evolution.dropfile.store.store.file.FileProvider;
import com.evolution.dropfile.store.store.file.FileProviderImpl;
import com.evolution.dropfile.store.store.file.SynchronizedFileKeyValueStore;
import com.evolution.dropfile.store.store.single.DefaultSingleValueStore;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CryptoSecretsConfigStore
        extends DefaultSingleValueStore<SecretsConfig>
        implements SecretsConfigStore {

    public CryptoSecretsConfigStore(ObjectMapper objectMapper,
                                    CryptoTunnel cryptoTunnel) {
        FileProvider fileProvider = new FileProviderImpl("secrets.config.json");
        super(
                "secretsConfig",
                new SynchronizedFileKeyValueStore<>(
                        fileProvider,
                        new CryptoFileOperations<>(
                                objectMapper,
                                SecretsConfig.class,
                                fileProvider,
                                cryptoTunnel
                        )
                )
        );
    }
}

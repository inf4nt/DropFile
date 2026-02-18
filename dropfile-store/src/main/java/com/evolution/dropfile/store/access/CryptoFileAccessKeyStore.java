package com.evolution.dropfile.store.access;

import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.store.store.file.CryptoFileOperations;
import com.evolution.dropfile.store.store.file.FileProvider;
import com.evolution.dropfile.store.store.file.FileProviderImpl;
import com.evolution.dropfile.store.store.file.SynchronizedFileKeyValueStore;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CryptoFileAccessKeyStore
        extends SynchronizedFileKeyValueStore<AccessKey>
        implements AccessKeyStore {

    public CryptoFileAccessKeyStore(ObjectMapper objectMapper, CryptoTunnel cryptoTunnel) {
        FileProvider fileProvider = new FileProviderImpl("access.keys.config.json");
        super(
                fileProvider,
                new CryptoFileOperations<>(
                        objectMapper,
                        AccessKey.class,
                        fileProvider,
                        cryptoTunnel
                )
        );
    }
}

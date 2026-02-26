package com.evolution.dropfile.store.access;

import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.store.framework.file.CryptoFileOperations;
import com.evolution.dropfile.store.framework.file.FileProvider;
import com.evolution.dropfile.store.framework.file.FileProviderImpl;
import com.evolution.dropfile.store.framework.file.SynchronizedFileKeyValueStore;
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

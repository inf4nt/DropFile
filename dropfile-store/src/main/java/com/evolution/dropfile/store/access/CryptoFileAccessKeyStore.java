package com.evolution.dropfile.store.access;

import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.store.framework.file.CryptoFileOperations;
import com.evolution.dropfile.store.framework.file.FileProvider;
import com.evolution.dropfile.store.framework.file.FileProviderImpl;
import com.evolution.dropfile.store.framework.file.SynchronizedFileKeyValueStore;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;

public class CryptoFileAccessKeyStore
        extends SynchronizedFileKeyValueStore<AccessKey>
        implements AccessKeyStore {

    public CryptoFileAccessKeyStore(ObjectMapper objectMapper, CryptoTunnel cryptoTunnel, Path parrentDirectoryPath) {
        FileProvider fileProvider = new FileProviderImpl(parrentDirectoryPath, "access.keys.config.json");
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

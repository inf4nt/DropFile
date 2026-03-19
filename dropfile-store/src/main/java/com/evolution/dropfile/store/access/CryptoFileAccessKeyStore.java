package com.evolution.dropfile.store.access;

import com.evolution.dropfile.common.FileHelper;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.store.framework.file.CacheableSynchronizedFileKeyValueStore;
import com.evolution.dropfile.store.framework.file.CryptoFileOperations;
import com.evolution.dropfile.store.framework.file.FileProvider;
import com.evolution.dropfile.store.framework.file.FileProviderImpl;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;

public class CryptoFileAccessKeyStore
        extends CacheableSynchronizedFileKeyValueStore<AccessKey>
        implements AccessKeyStore {

    public CryptoFileAccessKeyStore(FileHelper fileHelper, ObjectMapper objectMapper, CryptoTunnel cryptoTunnel, Path parrentDirectoryPath) {
        FileProvider fileProvider = new FileProviderImpl(parrentDirectoryPath, "access.keys.config.json");
        super(
                fileProvider,
                new CryptoFileOperations<>(
                        fileHelper,
                        objectMapper,
                        AccessKey.class,
                        fileProvider,
                        cryptoTunnel
                )
        );
    }
}

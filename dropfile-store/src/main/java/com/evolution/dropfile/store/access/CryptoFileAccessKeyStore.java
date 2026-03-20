package com.evolution.dropfile.store.access;

import com.evolution.dropfile.common.FileHelper;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.store.framework.file.ApplicationFingerprintSupplier;
import com.evolution.dropfile.store.framework.file.CacheableSynchronizedFileKeyValueStore;
import com.evolution.dropfile.store.framework.file.CryptoFileOperations;
import com.evolution.dropfile.store.framework.file.FileProviderImpl;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;

public class CryptoFileAccessKeyStore
        extends CacheableSynchronizedFileKeyValueStore<AccessKey>
        implements AccessKeyStore {

    public CryptoFileAccessKeyStore(FileHelper fileHelper, ObjectMapper objectMapper, CryptoTunnel cryptoTunnel,
                                    ApplicationFingerprintSupplier applicationFingerprintSupplier, Path parrentDirectoryPath) {
        super(
                new FileProviderImpl(parrentDirectoryPath, "access.keys.config.json"),
                new CryptoFileOperations<>(
                        fileHelper,
                        objectMapper,
                        AccessKey.class,
                        cryptoTunnel,
                        applicationFingerprintSupplier
                )
        );
    }
}

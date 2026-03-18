package com.evolution.dropfile.store.access;

import com.evolution.dropfile.common.FileHelper;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.store.framework.CacheableKeyValueStore;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;

public class CacheableCryptoFileAccessKeyStore
        extends CacheableKeyValueStore<AccessKey>
        implements AccessKeyStore {

    public CacheableCryptoFileAccessKeyStore(FileHelper fileHelper, ObjectMapper objectMapper, CryptoTunnel cryptoTunnel, Path parrentDirectoryPath) {
        super(new CryptoFileAccessKeyStore(fileHelper, objectMapper, cryptoTunnel, parrentDirectoryPath));
    }
}

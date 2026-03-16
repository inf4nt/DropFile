package com.evolution.dropfile.store.access;

import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.store.framework.CacheableKeyValueStore;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;

public class CacheableCryptoFileAccessKeyStore
        extends CacheableKeyValueStore<AccessKey>
        implements AccessKeyStore {

    public CacheableCryptoFileAccessKeyStore(ObjectMapper objectMapper, CryptoTunnel cryptoTunnel, Path parrentDirectoryPath) {
        super(new CryptoFileAccessKeyStore(objectMapper, cryptoTunnel, parrentDirectoryPath));
    }
}

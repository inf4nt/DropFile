package com.evolution.dropfile.store.secret;

import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.store.framework.single.CacheableSingleValueStore;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;

public class CacheableCryptoDaemonSecretsStore
        extends CacheableSingleValueStore<DaemonSecrets>
        implements DaemonSecretsStore {

    public CacheableCryptoDaemonSecretsStore(ObjectMapper objectMapper, CryptoTunnel cryptoTunnel, Path parrentDirectoryPath) {
        super(new CryptoDaemonSecretsStore(objectMapper, cryptoTunnel, parrentDirectoryPath));
    }
}

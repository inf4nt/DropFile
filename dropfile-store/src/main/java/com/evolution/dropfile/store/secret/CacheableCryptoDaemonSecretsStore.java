package com.evolution.dropfile.store.secret;

import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.store.framework.single.CacheableSingleValueStore;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CacheableCryptoDaemonSecretsStore
        extends CacheableSingleValueStore<DaemonSecrets>
        implements DaemonSecretsStore {

    public CacheableCryptoDaemonSecretsStore(ObjectMapper objectMapper, CryptoTunnel cryptoTunnel) {
        super(new CryptoDaemonSecretsStore(objectMapper, cryptoTunnel));
    }
}

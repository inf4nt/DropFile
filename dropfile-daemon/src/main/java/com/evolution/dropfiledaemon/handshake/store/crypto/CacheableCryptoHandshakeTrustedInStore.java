package com.evolution.dropfiledaemon.handshake.store.crypto;

import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.store.framework.CacheableKeyValueStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedInStore;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CacheableCryptoHandshakeTrustedInStore
        extends CacheableKeyValueStore<HandshakeTrustedInStore.TrustedIn>
        implements HandshakeTrustedInStore {
    public CacheableCryptoHandshakeTrustedInStore(ObjectMapper objectMapper, CryptoTunnel cryptoTunnel) {
        super(new CryptoHandshakeTrustedInStore(objectMapper, cryptoTunnel));
    }
}

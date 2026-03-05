package com.evolution.dropfiledaemon.handshake.store.crypto;

import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.store.framework.CacheableKeyValueStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedOutStore;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CacheableCryptoHandshakeTrustedOutStore
        extends CacheableKeyValueStore<HandshakeTrustedOutStore.TrustedOut>
        implements HandshakeTrustedOutStore {

    public CacheableCryptoHandshakeTrustedOutStore(ObjectMapper objectMapper, CryptoTunnel cryptoTunnel) {
        super(new CryptoHandshakeTrustedOutStore(objectMapper, cryptoTunnel));
    }
}

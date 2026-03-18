package com.evolution.dropfiledaemon.handshake.store.crypto;

import com.evolution.dropfile.common.FileHelper;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.store.framework.CacheableKeyValueStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedOutStore;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Path;

public class CacheableCryptoHandshakeTrustedOutStore
        extends CacheableKeyValueStore<HandshakeTrustedOutStore.TrustedOut>
        implements HandshakeTrustedOutStore {

    public CacheableCryptoHandshakeTrustedOutStore(FileHelper fileHelper, ObjectMapper objectMapper, CryptoTunnel cryptoTunnel, Path parrentDirectoryPath) {
        super(new CryptoHandshakeTrustedOutStore(fileHelper, objectMapper, cryptoTunnel, parrentDirectoryPath));
    }
}

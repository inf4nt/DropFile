package com.evolution.dropfiledaemon.handshake.store;

import com.evolution.dropfile.configuration.store.KeyValueStore;

import java.net.URI;

public interface TrustedOutKeyValueStore
        extends KeyValueStore<String, TrustedOutKeyValueStore.TrustedOutValue> {

    record TrustedOutValue(URI addressURI, byte[] publicKey, String secret) {
    }
}

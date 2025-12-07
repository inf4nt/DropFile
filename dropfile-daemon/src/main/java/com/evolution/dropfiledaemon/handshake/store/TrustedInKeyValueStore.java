package com.evolution.dropfiledaemon.handshake.store;

import com.evolution.dropfile.configuration.store.KeyValueStore;

import java.net.URI;

public interface TrustedInKeyValueStore
        extends KeyValueStore<String, TrustedInKeyValueStore.TrustedInValue> {

    record TrustedInValue(URI addressURI, byte[] publicKey, byte[] secret) {
    }
}

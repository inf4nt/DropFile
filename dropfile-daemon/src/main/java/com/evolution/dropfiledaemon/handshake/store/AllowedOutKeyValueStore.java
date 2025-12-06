package com.evolution.dropfiledaemon.handshake.store;

import com.evolution.dropfile.configuration.store.KeyValueStore;

import java.net.URI;

public interface AllowedOutKeyValueStore
        extends KeyValueStore<String, AllowedOutKeyValueStore.AllowedOutValue> {

    record AllowedOutValue(URI addressURI, byte[] publicKey, byte[] secret) {
    }
}

package com.evolution.dropfiledaemon.handshake.store;

import com.evolution.dropfile.configuration.store.KeyValueStore;

import java.net.URI;

public interface AllowedInKeyValueStore
        extends KeyValueStore<String, AllowedInKeyValueStore.AllowedInValue> {

    record AllowedInValue(URI addressURI, byte[] publicKey, byte[] secret) {
    }
}

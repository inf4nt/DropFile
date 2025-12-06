package com.evolution.dropfiledaemon.handshake.store;

import com.evolution.dropfile.configuration.store.KeyValueStore;

import java.net.URI;

public interface IncomingRequestKeyValueStore
        extends KeyValueStore<String, IncomingRequestKeyValueStore.IncomingRequestValue> {

    record IncomingRequestValue(URI addressURI, byte[] publicKey) {
    }
}

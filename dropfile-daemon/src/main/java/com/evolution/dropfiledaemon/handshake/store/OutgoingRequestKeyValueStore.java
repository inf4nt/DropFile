package com.evolution.dropfiledaemon.handshake.store;

import com.evolution.dropfile.configuration.store.KeyValueStore;

import java.net.URI;

public interface OutgoingRequestKeyValueStore
        extends KeyValueStore<String, OutgoingRequestKeyValueStore.OutgoingRequestValue> {

    record OutgoingRequestValue(URI addressURI, byte[] publicKey) {
    }
}

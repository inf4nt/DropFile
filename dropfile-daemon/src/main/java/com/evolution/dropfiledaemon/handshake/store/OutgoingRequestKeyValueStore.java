package com.evolution.dropfiledaemon.handshake.store;

import com.evolution.dropfile.configuration.store.KeyValueStore;

import java.net.URI;

@Deprecated
public interface OutgoingRequestKeyValueStore
        extends KeyValueStore<String, OutgoingRequestKeyValueStore.OutgoingRequestValue> {

    @Deprecated
    record OutgoingRequestValue(URI addressURI, byte[] publicKeyRSA, byte[] publicKeyDH) {
    }
}

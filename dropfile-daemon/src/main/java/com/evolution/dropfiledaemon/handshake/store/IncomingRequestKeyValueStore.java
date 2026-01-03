package com.evolution.dropfiledaemon.handshake.store;

import com.evolution.dropfile.configuration.store.KeyValueStore;

import java.net.URI;

@Deprecated
public interface IncomingRequestKeyValueStore
        extends KeyValueStore<String, IncomingRequestKeyValueStore.IncomingRequestValue> {

    @Deprecated
    record IncomingRequestValue(byte[] publicKeyRSA, byte[] publicKeyDH) {
    }
}

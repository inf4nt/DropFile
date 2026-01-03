package com.evolution.dropfiledaemon.handshake.store;

import com.evolution.dropfile.configuration.store.KeyValueStore;

import java.net.URI;
import java.time.Instant;

public interface TrustedOutKeyValueStore
        extends KeyValueStore<String, TrustedOutKeyValueStore.TrustedOutValue> {

    record TrustedOutValue(URI addressURI, byte[] publicKeyDH, Instant updated) {
    }
}

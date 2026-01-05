package com.evolution.dropfiledaemon.handshake.store;

import com.evolution.dropfile.store.store.KeyValueStore;

import java.time.Instant;

public interface TrustedInKeyValueStore
        extends KeyValueStore<String, TrustedInKeyValueStore.TrustedInValue> {

    record TrustedInValue(byte[] publicKeyRSA, byte[] publicKeyDH, Instant updated) {
    }
}

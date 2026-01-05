package com.evolution.dropfiledaemon.handshake.store;

import com.evolution.dropfile.store.store.KeyValueStore;

public interface TrustedInKeyValueStore
        extends KeyValueStore<String, TrustedInKeyValueStore.TrustedInValue> {

    record TrustedInValue(byte[] publicKeyRSA, byte[] publicKeyDH) {
    }
}

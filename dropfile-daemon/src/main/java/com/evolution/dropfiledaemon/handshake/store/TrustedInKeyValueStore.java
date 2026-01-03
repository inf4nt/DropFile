package com.evolution.dropfiledaemon.handshake.store;

import com.evolution.dropfile.configuration.store.KeyValueStore;

public interface TrustedInKeyValueStore
        extends KeyValueStore<String, TrustedInKeyValueStore.TrustedInValue> {

    record TrustedInValue(@Deprecated byte[] publicKeyRSA, byte[] publicKeyDH) {
    }
}

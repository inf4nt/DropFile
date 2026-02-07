package com.evolution.dropfiledaemon.handshake.store;

import com.evolution.dropfile.store.store.KeyValueStore;

import java.time.Instant;
import java.util.Comparator;
import java.util.Map;

public interface TrustedInKeyValueStore
        extends KeyValueStore<String, TrustedInKeyValueStore.TrustedInValue> {

    record TrustedInValue(byte[] publicKeyRSA, byte[] publicKeyDH, Instant updated) {
    }

    default Map.Entry<String, TrustedInValue> getRequiredLatestUpdated() {
        return getAll()
                .entrySet()
                .stream()
                .max(Comparator.comparing(o -> o.getValue().updated()))
                .orElseThrow(() -> new RuntimeException("No trusted-in found"));
    }
}

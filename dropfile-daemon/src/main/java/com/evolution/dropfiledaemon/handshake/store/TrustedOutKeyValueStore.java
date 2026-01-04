package com.evolution.dropfiledaemon.handshake.store;

import com.evolution.dropfile.configuration.store.KeyValueStore;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

public interface TrustedOutKeyValueStore
        extends KeyValueStore<String, TrustedOutKeyValueStore.TrustedOutValue> {

    record TrustedOutValue(URI addressURI, byte[] publicKeyDH, Instant updated) {
    }

    @Override
    default void validate(String key, TrustedOutValue value) {
        Map.Entry<String, TrustedOutValue> entry = getAll().entrySet()
                .stream()
                .filter(it -> it.getValue().addressURI().equals(value.addressURI()))
                .filter(it -> !it.getKey().equals(key))
                .findAny()
                .orElse(null);
        if (entry != null) {
            throw new RuntimeException(
                    String.format("Duplicate address URI %s fingerprint %s", value.addressURI(), entry.getKey())
            );
        }
    }
}

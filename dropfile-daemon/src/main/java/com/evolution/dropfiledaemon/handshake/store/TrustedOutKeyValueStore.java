package com.evolution.dropfiledaemon.handshake.store;

import com.evolution.dropfile.store.store.KeyValueStore;

import java.net.URI;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public interface TrustedOutKeyValueStore
        extends KeyValueStore<TrustedOutKeyValueStore.TrustedOutValue> {

    record TrustedOutValue(URI addressURI, byte[] publicKeyRSA, byte[] publicKeyDH, Instant updated) {
    }

    default Map.Entry<String, TrustedOutValue> getRequiredByAddressURI(URI addressURI) {
        List<Map.Entry<String, TrustedOutValue>> list = getAll().entrySet()
                .stream().filter(it -> it.getValue().addressURI().equals(addressURI))
                .toList();
        if (list.isEmpty()) {
            throw new RuntimeException("No trusted out value found for address: " + addressURI);
        }
        if (list.size() > 1) {
            throw new RuntimeException("More than one trusted out value found for address: " + addressURI);
        }
        return list.getFirst();
    }

    default Map.Entry<String, TrustedOutValue> getRequiredLatestUpdated() {
        return getAll()
                .entrySet()
                .stream()
                .max(Comparator.comparing(o -> o.getValue().updated()))
                .orElseThrow(() -> new RuntimeException("No trusted-out found"));
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
                    String.format("Duplicate addressURI. Fingerprint: %s URI: %s", entry.getKey(), value.addressURI())
            );
        }
    }
}

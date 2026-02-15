package com.evolution.dropfiledaemon.handshake.store;

import com.evolution.dropfile.store.store.KeyValueStore;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

public interface HandshakeTrustedOutStore extends KeyValueStore<HandshakeTrustedOutStore.TrustedOut> {

    record TrustedOut(URI addressURI,
                      byte[] publicRSA,
                      byte[] privateRSA,
                      byte[] remoteRSA,
                      Instant created) {

    }

    default Map.Entry<String, TrustedOut> getRequiredByAddressURI(URI addressURI) {
        return getAll().entrySet().stream()
                .filter(entry -> entry.getValue().addressURI.equals(addressURI))
                .findAny()
                .orElseThrow(() -> new RuntimeException("No trusted out value found for address: " + addressURI));
    }

    @Override
    default void validate(String key, TrustedOut value) {
        Map.Entry<String, TrustedOut> duplicateAddressURI = getAll().entrySet().stream()
                .filter(entry -> !entry.getKey().equals(key))
                .filter(entry -> entry.getValue().addressURI().equals(value.addressURI()))
                .findAny()
                .orElse(null);
        if (duplicateAddressURI != null) {
            throw new RuntimeException(
                    String.format("Duplicate addressURI %s Fingerprint: %s", value.addressURI(), duplicateAddressURI.getKey())
            );
        }
    }
}

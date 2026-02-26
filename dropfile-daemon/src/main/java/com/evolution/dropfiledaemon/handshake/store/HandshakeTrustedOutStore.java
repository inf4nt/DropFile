package com.evolution.dropfiledaemon.handshake.store;

import com.evolution.dropfile.store.framework.KeyValueStore;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public interface HandshakeTrustedOutStore extends KeyValueStore<HandshakeTrustedOutStore.TrustedOut> {

    record TrustedOut(URI addressURI,
                      byte[] publicRSA,
                      byte[] privateRSA,
                      byte[] remoteRSA,
                      Instant created) {

    }

    default Optional<Map.Entry<String, TrustedOut>> getByAddressURI(URI addressURI) {
        return getAll().entrySet().stream()
                .filter(entry -> entry.getValue().addressURI.equals(addressURI))
                .findAny();
    }

    default Map.Entry<String, TrustedOut> getRequiredByAddressURI(URI addressURI) {
        return getByAddressURI(addressURI)
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

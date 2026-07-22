package com.evolution.dropfiledaemon.handshake.store;

import com.evolution.dropfile.store.framework.KeyValueStore;
import lombok.With;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

public interface HandshakeTrustedOutStore extends KeyValueStore<HandshakeTrustedOutStore.TrustedOut> {

    @With
    record TrustedOut(URI addressURI,
                      HandshakeKeys handshake,
                      SessionKeys session,
                      Duration handshakeTtl,
                      Duration sessionTtl,
                      Instant created,
                      long sessionRefreshRequestTimestamp,
                      Instant sessionUpdatedByUser,
                      Instant sessionUpdatedBySystem,
                      Instant updated) {
    }

    record HandshakeKeys(byte[] publicRSA,
                         byte[] privateRSA,
                         byte[] remoteRSA) {
    }

    record SessionKeys(byte[] publicDH,
                       byte[] privateDH,
                       byte[] remotePublicDH) {
    }

    default Map.Entry<String, TrustedOut> getRequiredLastUpdated() {
        return getAll().entrySet()
                .stream()
                .max(Comparator.comparing(o -> o.getValue().updated()))
                .orElseThrow(() -> new RuntimeException("No trusted in found"));
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

package com.evolution.dropfiledaemon.handshake.store.api;

import com.evolution.dropfile.store.framework.KeyValueStore;
import jakarta.annotation.Nullable;
import lombok.With;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public interface HandshakeTrustedOutStore extends KeyValueStore<HandshakeTrustedOutStore.TrustedOut> {

    @With
    record TrustedOut(URI addressURI,
                      @Nullable String alias,
                      HandshakeKeys handshake,
                      Instant sessionUpdatedByUser,
                      Instant sessionUpdatedBySystem,
                      Instant created,
                      Instant updated,
                      UUID handshakeId) {
    }

    record HandshakeKeys(byte[] publicRSA,
                         byte[] privateRSA,
                         byte[] remoteRSA) {
    }

    default Map.Entry<String, TrustedOut> getRequiredLastUpdated() {
        return getAll().entrySet()
                .stream()
                .max(Comparator.comparing(o -> o.getValue().updated()))
                .orElseThrow(() -> new NoSuchElementException("No trusted out last updated found. The trusted out store is empty"));
    }

    default Optional<Map.Entry<String, TrustedOut>> getByAddressURI(URI addressURI) {
        return getAll().entrySet().stream()
                .filter(entry -> entry.getValue().addressURI.equals(addressURI))
                .findAny();
    }

    default Map.Entry<String, TrustedOut> getRequiredByAddressURI(URI addressURI) {
        return getByAddressURI(addressURI)
                .orElseThrow(() -> new NoSuchElementException("No trusted out value found for address: " + addressURI));
    }

    default Map<String, TrustedOut> getRequiredByAlias(String alias) {
        AliasValidator.validateOrThrow(alias);

        Map<String, TrustedOut> all = getAll();

        if (CollectionUtils.isEmpty(all)) {
            throw new NoSuchElementException("No trusted out value found");
        }

        Map<String, TrustedOut> elements = all.entrySet()
                .stream()
                .filter(entry -> alias.equals(entry.getValue().alias()))
                .collect(Collectors.toMap(
                        it -> it.getKey(),
                        it -> it.getValue()
                ));

        if (CollectionUtils.isEmpty(elements)) {
            throw new NoSuchElementException("No alias '%s' found".formatted(alias));
        }

        return elements;
    }

    @Override
    default void validate(String key, TrustedOut value) {
        if (StringUtils.hasText(value.alias())) {
            AliasValidator.validateOrThrow(value.alias());
        }

        Map.Entry<String, TrustedOut> duplicateAddressURI = getAll().entrySet().stream()
                .filter(entry -> !entry.getKey().equals(key))
                .filter(entry -> entry.getValue().addressURI().equals(value.addressURI()))
                .findAny()
                .orElse(null);
        if (duplicateAddressURI != null) {
            throw new IllegalArgumentException(
                    String.format("Duplicate addressURI %s Fingerprint: %s", value.addressURI(), duplicateAddressURI.getKey())
            );
        }
    }
}

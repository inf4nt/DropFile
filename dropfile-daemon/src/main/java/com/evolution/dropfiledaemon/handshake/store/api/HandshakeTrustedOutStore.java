package com.evolution.dropfiledaemon.handshake.store.api;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.CriteriaEnvelope;
import com.evolution.dropfile.store.framework.KeyValueStore;
import jakarta.annotation.Nullable;
import lombok.With;

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

    default Map<String, TrustedOut> getRequiredByCriteriaAlias(CriteriaEnvelope criteriaEnvelopeAlias) {
        CommonUtils.MatchResult<Map.Entry<String, TrustedOut>> matchResult = CommonUtils.matchBy(
                getAll().entrySet(),
                List.of(criteriaEnvelopeAlias),
                (criteriaEnvelope, entry) -> entry.getValue().alias() != null
                        && entry.getValue().alias().startsWith(criteriaEnvelope.value())
        );

        if (!matchResult.notFound().isEmpty()) {
            throw new NoSuchElementException(
                    "Store %s. No aliases found for criteria: %s".formatted(getClass().getSimpleName(), criteriaEnvelopeAlias.value())
            );
        }

        if (!matchResult.found().isEmpty() && matchResult.ambiguous().isEmpty()) {
            return matchResult.found().values()
                    .stream()
                    .collect(Collectors.toMap(
                            it -> it.getKey(),
                            it -> it.getValue()
                    ));
        }

        List<Map.Entry<String, TrustedOut>> matches = matchResult.ambiguous().get(criteriaEnvelopeAlias);
        int matchesCount = (matches != null) ? matches.size() : 0;

        throw new IllegalStateException(
                "Store %s. Ambiguous aliases criteria '%s'. Found %d matches".formatted(
                        getClass().getSimpleName(), criteriaEnvelopeAlias.value(), matchesCount
                )
        );
    }

    @Override
    default void validate(String key, TrustedOut value) {
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

package com.evolution.dropfiledaemon.handshake.store.api;

import com.evolution.dropfile.store.framework.KeyValueStore;
import lombok.With;

import java.net.URI;
import java.time.Instant;
import java.util.*;

public interface HandshakeTrustedOutStore extends KeyValueStore<HandshakeTrustedOutStore.TrustedOut> {

    @With
    record TrustedOut(List<AddressEnvelope> addressEnvelopes,
                      HandshakeKeys handshake,
                      Instant created,
                      Instant updated,
                      UUID handshakeId) {
        @Override
        public List<AddressEnvelope> addressEnvelopes() {
            return addressEnvelopes.stream()
                    .sorted((o1, o2) -> o2.lastSeen.compareTo(o1.lastSeen))
                    .toList();
        }
    }

    record HandshakeKeys(byte[] publicRSA,
                         byte[] privateRSA,
                         byte[] remoteRSA) {
    }

    @With
    record AddressEnvelope(URI address,
                           Instant lastSeen) {
    }

    default Map.Entry<String, TrustedOut> getRequiredLastUpdated() {
        return getAll().entrySet()
                .stream()
                .max(Comparator.comparing(o -> o.getValue().updated()))
                .orElseThrow(() -> new NoSuchElementException("No trusted out last updated found. The trusted out store is empty"));
    }
}

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
                      UUID sessionId) {
        public TrustedOut(List<AddressEnvelope> addressEnvelopes, HandshakeKeys handshake, Instant created, Instant updated, UUID sessionId) {
            this.addressEnvelopes = Objects.requireNonNull(addressEnvelopes);
            this.handshake = Objects.requireNonNull(handshake);
            this.created = Objects.requireNonNull(created);
            this.updated = Objects.requireNonNull(updated);
            this.sessionId = Objects.requireNonNull(sessionId);
            this.addressEnvelopes.forEach(Objects::requireNonNull);
        }

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
        public HandshakeKeys(byte[] publicRSA, byte[] privateRSA, byte[] remoteRSA) {
            this.publicRSA = Objects.requireNonNull(publicRSA);
            this.privateRSA = Objects.requireNonNull(privateRSA);
            this.remoteRSA = Objects.requireNonNull(remoteRSA);
        }
    }

    record AddressEnvelope(URI address,
                           Instant lastSeen) {
        public AddressEnvelope(URI address, Instant lastSeen) {
            this.address = Objects.requireNonNull(address);
            this.lastSeen = Objects.requireNonNull(lastSeen);
        }
    }

    default Map.Entry<String, TrustedOut> getRequiredLastUpdated() {
        return getAll().entrySet()
                .stream()
                .max(Comparator.comparing(o -> o.getValue().updated()))
                .orElseThrow(() -> new NoSuchElementException("No trusted out last updated found. The trusted out store is empty"));
    }
}

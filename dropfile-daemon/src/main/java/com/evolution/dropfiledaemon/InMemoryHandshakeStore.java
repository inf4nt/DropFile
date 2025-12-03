package com.evolution.dropfiledaemon;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class InMemoryHandshakeStore {

    private final Map<String, HandshakeRequestEnvelope> requests = new HashMap<>();

    private final Map<String, HandshakeEnvelope> trusted = new HashMap<>();

    public void requestToTrusted(String fingerPrint, String secret) {
        HandshakeRequestEnvelope request = requests.get(fingerPrint);
        Objects.requireNonNull(request);
        requests.remove(fingerPrint);
        trusted.put(fingerPrint, new HandshakeEnvelope(request.publicKey, secret));
    }

    public void addTrusted(String fingerPrint, byte[] publicKey, String secret) {
        trusted.put(fingerPrint, new HandshakeEnvelope(publicKey, secret));
    }

    public HandshakeRequestEnvelope getRequest(String fingerprint) {
        return requests.get(fingerprint);
    }

    public void addRequest(String fingerPrint,
                           byte[] publicKey,
                           URI nodeAddressURI) {
        requests.put(fingerPrint, new HandshakeRequestEnvelope(publicKey, nodeAddressURI));
    }

    public Map<String, HandshakeRequestEnvelope> getRequests() {
        return Collections.unmodifiableMap(requests);
    }

    public Map<String, HandshakeEnvelope> getTrusted() {
        return Collections.unmodifiableMap(trusted);
    }

    public record HandshakeRequestEnvelope(byte[] publicKey, URI addressURI) {

    }

    public record HandshakeEnvelope(byte[] publicKey, String secret) {

    }
}

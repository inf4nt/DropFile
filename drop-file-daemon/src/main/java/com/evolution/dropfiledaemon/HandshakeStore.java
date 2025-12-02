package com.evolution.dropfiledaemon;

import com.evolution.dropfile.configuration.crypto.CryptoUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class HandshakeStore {

    private final Map<String, String> handshakes = new HashMap<>();

    public void add(byte[] publicKey, String secretKey) {
        String fingerPrint = CryptoUtils.getFingerPrint(publicKey);
        handshakes.put(fingerPrint, secretKey);
    }

    public Optional<String> getSecret(String fingerPrint) {
        return Optional.ofNullable(handshakes.get(fingerPrint));
    }

    public String getSecretRequired(String fingerPrint) {
        return Optional.ofNullable(handshakes.get(fingerPrint))
                .orElseThrow();
    }
}

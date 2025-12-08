package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.crypto.CryptoUtils;
import com.evolution.dropfile.common.dto.DaemonInfoResponseDTO;
import com.evolution.dropfile.configuration.keys.DropFileKeysConfig;
import com.evolution.dropfiledaemon.handshake.store.HandshakeStore;
import com.evolution.dropfiledaemon.handshake.store.TrustedOutKeyValueStore;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executors;

@Component
public class ApiFacade {

    private final DropFileKeysConfig keysConfig;

    private final HttpClient httpClient;

    private final HandshakeStore handshakeStore;

    @Autowired
    public ApiFacade(DropFileKeysConfig keysConfig,
                     HttpClient httpClient,
                     HandshakeStore handshakeStore) {
        this.keysConfig = keysConfig;
        this.httpClient = httpClient;
        this.handshakeStore = handshakeStore;
    }

    public void shutdown() {
        Executors.newSingleThreadExecutor()
                .submit(() -> {
                    System.exit(0);
                });
    }

    public DaemonInfoResponseDTO getDaemonInfo() {
        byte[] publicKey = keysConfig.getKeyPair().getPublic().getEncoded();
        String publicKeyBase64 = CryptoUtils.encodeBase64(publicKey);
        String fingerPrint = CryptoUtils.getFingerPrint(publicKey);

        return new DaemonInfoResponseDTO(fingerPrint, publicKeyBase64);
    }

    @SneakyThrows
    public String nodePing(String fingerprint) {
        TrustedOutKeyValueStore.TrustedOutValue trustedOutValue = handshakeStore
                .trustedOutStore()
                .get(fingerprint)
                .orElseThrow();
        byte[] encrypt = CryptoUtils.encrypt(trustedOutValue.publicKey(), trustedOutValue.secret().getBytes());
        String tokenBase64 = CryptoUtils.encodeBase64(encrypt);

        URI nodeAddressURI = trustedOutValue.addressURI()
                .resolve("/node/ping");

        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(nodeAddressURI)
                .header("Authorization", "Bearer "  + tokenBase64)
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }
}

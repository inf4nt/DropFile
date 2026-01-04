package com.evolution.dropfiledaemon.handshake;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.crypto.CryptoECDH;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.common.crypto.SecureEnvelope;
import com.evolution.dropfile.common.dto.*;
import com.evolution.dropfile.configuration.app.AppConfigStore;
import com.evolution.dropfile.configuration.keys.KeysConfigStore;
import com.evolution.dropfiledaemon.client.HandshakeClient;
import com.evolution.dropfiledaemon.handshake.store.HandshakeStore;
import com.evolution.dropfiledaemon.handshake.store.TrustedOutKeyValueStore;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.net.URI;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class ApiHandshakeFacade {

    private final HandshakeStore handshakeStore;

    private final HandshakeClient handshakeClient;

    private final KeysConfigStore keysConfigStore;

    private final CryptoTunnel cryptoTunnel;

    private final ObjectMapper objectMapper;

    @Autowired
    public ApiHandshakeFacade(HandshakeStore handshakeStore,
                              HandshakeClient handshakeClient,
                              KeysConfigStore keysConfigStore,
                              CryptoTunnel cryptoTunnel,
                              ObjectMapper objectMapper) {
        this.handshakeStore = handshakeStore;
        this.handshakeClient = handshakeClient;
        this.keysConfigStore = keysConfigStore;
        this.cryptoTunnel = cryptoTunnel;
        this.objectMapper = objectMapper;
    }

    @SneakyThrows
    public ApiHandshakeStatusResponseDTO handshakeReconnect(ApiHandshakeReconnectRequestDTO requestDTO) {
        TrustedOutKeyValueStore.TrustedOutValue trustedOutValue = handshakeStore.trustedOutStore().getAll().values().stream()
                .filter(it -> it.addressURI().equals(CommonUtils.toURI(requestDTO.address())))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No trusted-out found: " + requestDTO.address()));
        ApiHandshakeStatusResponseDTO apiHandshakeStatusResponseDTO = pingHandshake(trustedOutValue);
        handshakeStore.trustedOutStore().save(
                CommonUtils.getFingerprint(trustedOutValue.publicKeyDH()),
                new TrustedOutKeyValueStore.TrustedOutValue(
                        trustedOutValue.addressURI(),
                        trustedOutValue.publicKeyDH(),
                        Instant.now()
                )
        );
        return apiHandshakeStatusResponseDTO;
    }

    public ApiHandshakeStatusResponseDTO handshakeStatus() {
        TrustedOutKeyValueStore.TrustedOutValue trustedOutValue = getLatestTrustOut()
                .flatMap(it -> handshakeStore.trustedOutStore().get(it.fingerprint()))
                .orElseThrow(() -> new RuntimeException("No trusted-out connections found"));
        return pingHandshake(trustedOutValue);
    }

    @SneakyThrows
    public ApiHandshakeStatusResponseDTO pingHandshake(TrustedOutKeyValueStore.TrustedOutValue trustedOutValue) {
        byte[] secret = CryptoECDH.getSecretKey(
                CryptoECDH.getPrivateKey(keysConfigStore.getRequired().dh().privateKey()),
                CryptoECDH.getPublicKey(trustedOutValue.publicKeyDH())
        );
        SecretKey secretKey = cryptoTunnel.secretKey(secret);

        HandshakeRequestDTO.HandshakePayload requestPayload = new HandshakeRequestDTO.HandshakePayload(
                CommonUtils.encodeBase64(keysConfigStore.getRequired().dh().publicKey()),
                System.currentTimeMillis()
        );
        SecureEnvelope secureEnvelope = cryptoTunnel.encrypt(
                objectMapper.writeValueAsBytes(requestPayload),
                secretKey
        );

        String requestId = CommonUtils.getFingerprint(keysConfigStore.getRequired().dh().publicKey());
        HandshakeRequestDTO handshakeRequestDTO = new HandshakeRequestDTO(
                requestId,
                CommonUtils.encodeBase64(secureEnvelope.payload()),
                CommonUtils.encodeBase64(secureEnvelope.nonce())
        );

        URI addressURI = trustedOutValue.addressURI();

        HttpResponse<byte[]> handshakeResponse = handshakeClient
                .handshake(addressURI, handshakeRequestDTO);
        if (handshakeResponse.statusCode() != 200) {
            throw new RuntimeException("Unexpected handshake response: " + handshakeResponse.statusCode());
        }

        HandshakeResponseDTO handshakeResponseDTO = objectMapper.readValue(handshakeResponse.body(), HandshakeResponseDTO.class);

        byte[] decryptResponsePayload = cryptoTunnel.decrypt(
                CommonUtils.decodeBase64(handshakeResponseDTO.payload()),
                CommonUtils.decodeBase64(handshakeResponseDTO.nonce()),
                secretKey
        );
        HandshakeResponseDTO.HandshakePayload responsePayload = objectMapper.readValue(
                decryptResponsePayload,
                HandshakeResponseDTO.HandshakePayload.class
        );
        String fingerprint = CommonUtils.getFingerprint(CommonUtils.decodeBase64(responsePayload.publicKeyDH()));
        if (!CommonUtils.getFingerprint(trustedOutValue.publicKeyDH()).equals(fingerprint)) {
            throw new RuntimeException("Fingerprint mismatch: " + fingerprint);
        }

        if (Math.abs(System.currentTimeMillis() - responsePayload.timestamp()) > 30_000) {
            throw new RuntimeException("Timed out");
        }

        if (responsePayload.status() != HandshakeResponseDTO.HandshakeStatus.OK) {
            throw new RuntimeException("Unexpected handshake response: " + responsePayload.status());
        }

        return new ApiHandshakeStatusResponseDTO(
                fingerprint,
                addressURI.toString(),
                "Online",
                responsePayload.tunnelAlgorithm()
        );
    }

    @SneakyThrows
    public ApiHandshakeStatusResponseDTO handshake(ApiHandshakeRequestDTO requestDTO) {
        String key = new String(CommonUtils.decodeBase64(requestDTO.key()));
        String secret = extractSecret(key);

        HandshakeRequestDTO.HandshakePayload requestPayload = new HandshakeRequestDTO.HandshakePayload(
                CommonUtils.encodeBase64(keysConfigStore.getRequired().dh().publicKey()),
                System.currentTimeMillis()
        );
        SecretKey secretKey = cryptoTunnel.secretKey(secret.getBytes());
        SecureEnvelope secureEnvelope = cryptoTunnel.encrypt(
                objectMapper.writeValueAsBytes(requestPayload),
                secretKey
        );

        String requestId = extractSecretId(key);
        HandshakeRequestDTO handshakeRequestDTO = new HandshakeRequestDTO(
                requestId,
                CommonUtils.encodeBase64(secureEnvelope.payload()),
                CommonUtils.encodeBase64(secureEnvelope.nonce())
        );

        URI addressURI = CommonUtils.toURI(requestDTO.address());

        HttpResponse<byte[]> handshakeResponse = handshakeClient
                .handshake(addressURI, handshakeRequestDTO);
        if (handshakeResponse.statusCode() != 200) {
            throw new RuntimeException("Unexpected handshake response: " + handshakeResponse.statusCode());
        }

        HandshakeResponseDTO handshakeResponseDTO = objectMapper.readValue(handshakeResponse.body(), HandshakeResponseDTO.class);

        byte[] decryptResponsePayload = cryptoTunnel.decrypt(
                CommonUtils.decodeBase64(handshakeResponseDTO.payload()),
                CommonUtils.decodeBase64(handshakeResponseDTO.nonce()),
                secretKey
        );
        HandshakeResponseDTO.HandshakePayload responsePayload = objectMapper.readValue(
                decryptResponsePayload,
                HandshakeResponseDTO.HandshakePayload.class
        );

        if (Math.abs(System.currentTimeMillis() - responsePayload.timestamp()) > 30_000) {
            throw new RuntimeException("Timed out");
        }

        if (responsePayload.status() != HandshakeResponseDTO.HandshakeStatus.OK) {
            throw new RuntimeException("Unexpected handshake response: " + responsePayload.status());
        }

        handshakeStore.trustedOutStore().save(
                CommonUtils.getFingerprint(CommonUtils.decodeBase64(responsePayload.publicKeyDH())),
                new TrustedOutKeyValueStore.TrustedOutValue(
                        addressURI,
                        CommonUtils.decodeBase64(responsePayload.publicKeyDH()),
                        Instant.now()
                )
        );
        return new ApiHandshakeStatusResponseDTO(
                CommonUtils.getFingerprint(CommonUtils.decodeBase64(responsePayload.publicKeyDH())),
                addressURI.toString(),
                "Online",
                responsePayload.tunnelAlgorithm()
        );
    }

    private String extractSecretId(String key) {
        String[] split = key.split("\\+");
        return split[0];
    }

    private String extractSecret(String key) {
        String[] split = key.split("\\+");
        return split[1];
    }

    public List<HandshakeApiTrustInResponseDTO> getTrustIt() {
        return handshakeStore
                .trustedInStore()
                .getAll()
                .entrySet()
                .stream()
                .map(entry -> {
                    String fingerprint = entry.getKey();
                    String publicKeyDH = CommonUtils.encodeBase64(entry.getValue().publicKeyDH());
                    return new HandshakeApiTrustInResponseDTO(fingerprint, publicKeyDH);
                })
                .toList();
    }

    public List<HandshakeApiTrustOutResponseDTO> getTrustOut() {
        return handshakeStore
                .trustedOutStore()
                .getAll()
                .entrySet()
                .stream()
                .map(entry -> {
                    String fingerprint = entry.getKey();
                    URI addressURI = entry.getValue().addressURI();
                    String publicKeyDH = CommonUtils.encodeBase64(entry.getValue().publicKeyDH());
                    return new HandshakeApiTrustOutResponseDTO(fingerprint, publicKeyDH, addressURI.toString());
                })
                .toList();
    }

    public Optional<HandshakeApiTrustOutResponseDTO> getLatestTrustOut() {
        return handshakeStore.trustedOutStore()
                .getAll()
                .entrySet()
                .stream()
                .max(Comparator.comparing(o -> o.getValue().updated()))
                .map(it -> new HandshakeApiTrustOutResponseDTO(
                                it.getKey(),
                                CommonUtils.encodeBase64(it.getValue().publicKeyDH()),
                                it.getValue().addressURI().toString()
                        )
                );
    }
}

package com.evolution.dropfiledaemon.handshake;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.crypto.CryptoECDH;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.common.crypto.CryptoUtils;
import com.evolution.dropfile.common.crypto.SecureEnvelope;
import com.evolution.dropfile.common.dto.*;
import com.evolution.dropfile.configuration.app.AppConfigStore;
import com.evolution.dropfile.configuration.keys.KeysConfigStore;
import com.evolution.dropfiledaemon.client.HandshakeClient;
import com.evolution.dropfiledaemon.handshake.store.HandshakeStore;
import com.evolution.dropfiledaemon.handshake.store.TrustedOutKeyValueStore;
import com.evolution.dropfiledaemon.tunnel.TunnelClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.net.URI;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class ApiHandshakeFacade {

    private final HandshakeStore handshakeStore;

    private final HandshakeClient handshakeClient;

    private final ObjectMapper objectMapper;

    private final KeysConfigStore keysConfigStore;

    private final AppConfigStore appConfigStore;

    private final CryptoTunnel cryptoTunnel;

    private final TunnelClient tunnelClient;

    public ApiHandshakeFacade(HandshakeStore handshakeStore,
                              HandshakeClient handshakeClient,
                              ObjectMapper objectMapper,
                              KeysConfigStore keysConfigStore,
                              AppConfigStore appConfigStore,
                              CryptoTunnel cryptoTunnel,
                              TunnelClient tunnelClient) {
        this.handshakeStore = handshakeStore;
        this.handshakeClient = handshakeClient;
        this.objectMapper = objectMapper;
        this.keysConfigStore = keysConfigStore;
        this.appConfigStore = appConfigStore;
        this.cryptoTunnel = cryptoTunnel;
        this.tunnelClient = tunnelClient;
    }

    @SneakyThrows
    public HandshakeIdentityResponseDTO identity(String address) {
        URI addressURI = CommonUtils.toURI(address);
        HttpResponse<byte[]> identity = handshakeClient.getIdentity(addressURI);
        if (identity.statusCode() != 200) {
            throw new RuntimeException("Unexpected identity response: " + new String(identity.body()));
        }
        return objectMapper.readValue(identity.body(), HandshakeIdentityResponseDTO.class);
    }

    @SneakyThrows
    public ApiHandshakeStatusResponseDTO handshakeStatus() {
        HandshakeApiTrustOutResponseDTO currentConnection = getLatestTrustOut()
                .orElse(null);
        if (currentConnection == null) {
            throw new IllegalStateException("No connections found");
        }

        byte[] secret = CryptoECDH.getSecretKey(
                CryptoECDH.getPrivateKey(keysConfigStore.getRequired().dh().privateKey()),
                CryptoECDH.getPublicKey(CryptoUtils.decodeBase64(currentConnection.publicKeyDH()))
        );
        SecretKey secretKey = cryptoTunnel.secretKey(secret);

        HandshakeRequestDTO.HandshakePayload requestPayload = new HandshakeRequestDTO.HandshakePayload(
                CryptoUtils.encodeBase64(keysConfigStore.getRequired().dh().publicKey()),
                System.currentTimeMillis()
        );
        SecureEnvelope secureEnvelope = cryptoTunnel.encrypt(
                objectMapper.writeValueAsBytes(requestPayload),
                secretKey
        );

        String requestId = CryptoUtils.getFingerprint(keysConfigStore.getRequired().dh().publicKey());
        HandshakeRequestDTO handshakeRequestDTO = new HandshakeRequestDTO(
                requestId,
                CryptoUtils.encodeBase64(secureEnvelope.payload()),
                CryptoUtils.encodeBase64(secureEnvelope.nonce())
        );

        URI addressURI = CommonUtils.toURI(currentConnection.addressURI());

        HttpResponse<byte[]> handshakeResponse = handshakeClient
                .handshake(addressURI, handshakeRequestDTO);
        if (handshakeResponse.statusCode() != 200) {
            throw new RuntimeException("Unexpected handshake response: " + handshakeResponse.statusCode());
        }

        HandshakeResponseDTO handshakeResponseDTO = objectMapper.readValue(handshakeResponse.body(), HandshakeResponseDTO.class);

        byte[] decryptResponsePayload = cryptoTunnel.decrypt(
                CryptoUtils.decodeBase64(handshakeResponseDTO.payload()),
                CryptoUtils.decodeBase64(handshakeResponseDTO.nonce()),
                secretKey
        );
        HandshakeResponseDTO.HandshakePayload responsePayload = objectMapper.readValue(
                decryptResponsePayload,
                HandshakeResponseDTO.HandshakePayload.class
        );
        String fingerprintActual = CryptoUtils.getFingerprint(CryptoUtils.decodeBase64(responsePayload.publicKeyDH()));
        if (!currentConnection.fingerprint().equals(fingerprintActual)) {
            throw new RuntimeException("Fingerprint mismatch: " + fingerprintActual);
        }

        if (Math.abs(System.currentTimeMillis() - responsePayload.timestamp()) > 30_000) {
            throw new RuntimeException("Timed out");
        }

        if (responsePayload.status() != HandshakeResponseDTO.HandshakeStatus.OK) {
            throw new RuntimeException("Unexpected handshake response: " + responsePayload.status());
        }

        return new ApiHandshakeStatusResponseDTO(
                currentConnection.fingerprint(),
                addressURI.toString(),
                "Active",
                responsePayload.tunnelAlgorithm()
        );
    }

    @SneakyThrows
    public ApiHandshakeStatusResponseDTO handshake(ApiHandshakeRequestDTO requestDTO) {
        String key = new String(CryptoUtils.decodeBase64(requestDTO.key()));
        String secret = extractSecret(key);

        HandshakeRequestDTO.HandshakePayload requestPayload = new HandshakeRequestDTO.HandshakePayload(
                CryptoUtils.encodeBase64(keysConfigStore.getRequired().dh().publicKey()),
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
                CryptoUtils.encodeBase64(secureEnvelope.payload()),
                CryptoUtils.encodeBase64(secureEnvelope.nonce())
        );

        URI addressURI = CommonUtils.toURI(requestDTO.address());

        HttpResponse<byte[]> handshakeResponse = handshakeClient
                .handshake(addressURI, handshakeRequestDTO);
        if (handshakeResponse.statusCode() != 200) {
            throw new RuntimeException("Unexpected handshake response: " + handshakeResponse.statusCode());
        }

        HandshakeResponseDTO handshakeResponseDTO = objectMapper.readValue(handshakeResponse.body(), HandshakeResponseDTO.class);

        byte[] decryptResponsePayload = cryptoTunnel.decrypt(
                CryptoUtils.decodeBase64(handshakeResponseDTO.payload()),
                CryptoUtils.decodeBase64(handshakeResponseDTO.nonce()),
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
                CryptoUtils.getFingerprint(CryptoUtils.decodeBase64(responsePayload.publicKeyDH())),
                new TrustedOutKeyValueStore.TrustedOutValue(
                        addressURI,
                        CryptoUtils.decodeBase64(responsePayload.publicKeyDH()),
                        Instant.now()
                )
        );
        return new ApiHandshakeStatusResponseDTO(
                CryptoUtils.getFingerprint(CryptoUtils.decodeBase64(responsePayload.publicKeyDH())),
                addressURI.toString(),
                "Active",
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
                    String publicKeyDH = CryptoUtils.encodeBase64(entry.getValue().publicKeyDH());
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
                    String publicKeyDH = CryptoUtils.encodeBase64(entry.getValue().publicKeyDH());
                    return new HandshakeApiTrustOutResponseDTO(fingerprint, publicKeyDH, addressURI.toString());
                })
                .toList();
    }

    public Optional<HandshakeApiTrustOutResponseDTO> getLatestTrustOut() {
        return handshakeStore.trustedOutStore()
                .getAll()
                .entrySet()
                .stream()
                .sorted((o1, o2) -> o2.getValue().updated().compareTo(o1.getValue().updated()))
                .findFirst()
                .map(it -> new HandshakeApiTrustOutResponseDTO(
                                it.getKey(),
                                CryptoUtils.encodeBase64(it.getValue().publicKeyDH()),
                                it.getValue().addressURI().toString()
                        )
                );
    }
}

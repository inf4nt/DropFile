package com.evolution.dropfiledaemon.handshake;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.crypto.CryptoECDH;
import com.evolution.dropfile.common.crypto.CryptoRSA;
import com.evolution.dropfile.common.dto.*;
import com.evolution.dropfile.store.keys.KeysConfigStore;
import com.evolution.dropfiledaemon.handshake.client.HandshakeClient;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeRequestDTO;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeResponseDTO;
import com.evolution.dropfiledaemon.handshake.store.HandshakeStore;
import com.evolution.dropfiledaemon.handshake.store.TrustedOutKeyValueStore;
import com.evolution.dropfiledaemon.tunnel.CryptoTunnel;
import com.evolution.dropfiledaemon.tunnel.SecureEnvelope;
import com.evolution.dropfiledaemon.util.AccessKeyUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.net.URI;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;

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
                CommonUtils.getFingerprint(trustedOutValue.publicKeyRSA()),
                new TrustedOutKeyValueStore.TrustedOutValue(
                        trustedOutValue.addressURI(),
                        trustedOutValue.publicKeyRSA(),
                        trustedOutValue.publicKeyDH(),
                        Instant.now()
                )
        );
        return apiHandshakeStatusResponseDTO;
    }

    public ApiHandshakeStatusResponseDTO handshakeStatus() {
        TrustedOutKeyValueStore.TrustedOutValue trustedOutValue = handshakeStore.trustedOutStore()
                .getRequiredLatestUpdated()
                .getValue();
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
                keysConfigStore.getRequired().rsa().publicKey(),
                keysConfigStore.getRequired().dh().publicKey(),
                System.currentTimeMillis()
        );
        byte[] requestPayloadByteArray = objectMapper.writeValueAsBytes(requestPayload);
        SecureEnvelope secureEnvelope = cryptoTunnel.encrypt(
                requestPayloadByteArray,
                secretKey
        );

        byte[] signature = CryptoRSA.sign(
                requestPayloadByteArray,
                CryptoRSA.getPrivateKey(keysConfigStore.getRequired().rsa().privateKey())
        );
        HandshakeRequestDTO handshakeRequestDTO = new HandshakeRequestDTO(
                CommonUtils.getFingerprint(keysConfigStore.getRequired().rsa().publicKey()),
                secureEnvelope.payload(),
                secureEnvelope.nonce(),
                signature
        );

        URI addressURI = trustedOutValue.addressURI();

        HttpResponse<byte[]> handshakeResponse = handshakeClient
                .handshake(addressURI, handshakeRequestDTO);
        if (handshakeResponse.statusCode() != 200) {
            throw new RuntimeException("Unexpected handshake response: " + handshakeResponse.statusCode());
        }

        HandshakeResponseDTO handshakeResponseDTO = objectMapper.readValue(handshakeResponse.body(), HandshakeResponseDTO.class);

        byte[] decryptResponsePayload = cryptoTunnel.decrypt(
                handshakeResponseDTO.payload(),
                handshakeResponseDTO.nonce(),
                secretKey
        );
        HandshakeResponseDTO.HandshakePayload responsePayload = objectMapper.readValue(
                decryptResponsePayload,
                HandshakeResponseDTO.HandshakePayload.class
        );

        boolean verify = CryptoRSA.verify(
                decryptResponsePayload,
                handshakeResponseDTO.signature(),
                CryptoRSA.getPublicKey(responsePayload.publicKeyRSA())
        );
        if (!verify) {
            throw new RuntimeException("Signature verification failed");
        }

        String fingerprintResponse = CommonUtils.getFingerprint(responsePayload.publicKeyRSA());
        if (!CommonUtils.getFingerprint(trustedOutValue.publicKeyRSA()).equals(fingerprintResponse)) {
            throw new RuntimeException("Fingerprint mismatch: " + fingerprintResponse);
        }

        if (Math.abs(System.currentTimeMillis() - responsePayload.timestamp()) > 30_000) {
            throw new RuntimeException("Timed out");
        }

        if (responsePayload.status() != HandshakeResponseDTO.HandshakeStatus.OK) {
            throw new RuntimeException("Unexpected handshake response: " + responsePayload.status());
        }

        return new ApiHandshakeStatusResponseDTO(
                fingerprintResponse,
                addressURI.toString(),
                "Online",
                responsePayload.tunnelAlgorithm()
        );
    }

    @SneakyThrows
    public ApiHandshakeStatusResponseDTO handshake(ApiHandshakeRequestDTO requestDTO) {
        String secret = new String(CommonUtils.decodeBase64(requestDTO.key()));

        HandshakeRequestDTO.HandshakePayload requestPayload = new HandshakeRequestDTO.HandshakePayload(
                keysConfigStore.getRequired().rsa().publicKey(),
                keysConfigStore.getRequired().dh().publicKey(),
                System.currentTimeMillis()
        );
        byte[] requestPayloadByteArray = objectMapper.writeValueAsBytes(requestPayload);

        SecretKey secretKey = cryptoTunnel.secretKey(secret.getBytes());
        SecureEnvelope secureEnvelope = cryptoTunnel.encrypt(
                requestPayloadByteArray,
                secretKey
        );

        String requestId = AccessKeyUtils.getId(secret);
        byte[] signature = CryptoRSA.sign(
                requestPayloadByteArray,
                CryptoRSA.getPrivateKey(keysConfigStore.getRequired().rsa().privateKey())
        );
        HandshakeRequestDTO handshakeRequestDTO = new HandshakeRequestDTO(
                requestId,
                secureEnvelope.payload(),
                secureEnvelope.nonce(),
                signature
        );

        URI addressURI = CommonUtils.toURI(requestDTO.address());

        HttpResponse<byte[]> handshakeResponse = handshakeClient
                .handshake(addressURI, handshakeRequestDTO);
        if (handshakeResponse.statusCode() != 200) {
            throw new RuntimeException("Unexpected handshake response: " + handshakeResponse.statusCode());
        }

        HandshakeResponseDTO handshakeResponseDTO = objectMapper.readValue(handshakeResponse.body(), HandshakeResponseDTO.class);

        byte[] decryptResponsePayload = cryptoTunnel.decrypt(
                handshakeResponseDTO.payload(),
                handshakeResponseDTO.nonce(),
                secretKey
        );

        HandshakeResponseDTO.HandshakePayload responsePayload = objectMapper.readValue(
                decryptResponsePayload,
                HandshakeResponseDTO.HandshakePayload.class
        );
        boolean verify = CryptoRSA.verify(
                decryptResponsePayload,
                handshakeResponseDTO.signature(),
                CryptoRSA.getPublicKey(responsePayload.publicKeyRSA())
        );
        if (!verify) {
            throw new RuntimeException("Signature verification failed");
        }

        if (Math.abs(System.currentTimeMillis() - responsePayload.timestamp()) > 30_000) {
            throw new RuntimeException("Timed out");
        }

        if (responsePayload.status() != HandshakeResponseDTO.HandshakeStatus.OK) {
            throw new RuntimeException("Unexpected handshake response: " + responsePayload.status());
        }

        String fingerprint = CommonUtils.getFingerprint(responsePayload.publicKeyRSA());
        handshakeStore.trustedOutStore().save(
                fingerprint,
                new TrustedOutKeyValueStore.TrustedOutValue(
                        addressURI,
                        responsePayload.publicKeyRSA(),
                        responsePayload.publicKeyDH(),
                        Instant.now()
                )
        );
        return new ApiHandshakeStatusResponseDTO(
                fingerprint,
                addressURI.toString(),
                "Online",
                responsePayload.tunnelAlgorithm()
        );
    }

    public List<HandshakeApiTrustInResponseDTO> getTrustIt() {
        return handshakeStore
                .trustedInStore()
                .getAll()
                .entrySet()
                .stream()
                .map(entry -> new HandshakeApiTrustInResponseDTO(
                        entry.getKey(),
                        CommonUtils.encodeBase64(entry.getValue().publicKeyRSA()),
                        CommonUtils.encodeBase64(entry.getValue().publicKeyDH()),
                        entry.getValue().updated()
                ))
                .toList();
    }

    public List<HandshakeApiTrustOutResponseDTO> getTrustOut() {
        return handshakeStore
                .trustedOutStore()
                .getAll()
                .entrySet()
                .stream()
                .map(it -> toHandshakeApiTrustOutResponseDTO(it))
                .toList();
    }

    public HandshakeApiTrustOutResponseDTO getLatestTrustOut() {
        Map.Entry<String, TrustedOutKeyValueStore.TrustedOutValue> trustedOutEntry = handshakeStore.trustedOutStore()
                .getRequiredLatestUpdated();
        return toHandshakeApiTrustOutResponseDTO(trustedOutEntry);
    }

    private HandshakeApiTrustOutResponseDTO toHandshakeApiTrustOutResponseDTO(Map.Entry<String, TrustedOutKeyValueStore.TrustedOutValue> entry) {
        return new HandshakeApiTrustOutResponseDTO(
                entry.getKey(),
                CommonUtils.encodeBase64(entry.getValue().publicKeyRSA()),
                CommonUtils.encodeBase64(entry.getValue().publicKeyDH()),
                entry.getValue().addressURI().toString(),
                entry.getValue().updated()
        );
    }
}

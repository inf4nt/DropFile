package com.evolution.dropfiledaemon.handshake;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.crypto.CryptoECDH;
import com.evolution.dropfile.common.crypto.CryptoRSA;
import com.evolution.dropfile.common.dto.*;
import com.evolution.dropfiledaemon.configuration.ApplicationConfigStore;
import com.evolution.dropfiledaemon.handshake.client.HandshakeClient;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeRequestDTO;
import com.evolution.dropfiledaemon.handshake.dto.HandshakeResponseDTO;
import com.evolution.dropfiledaemon.handshake.store.TrustedOutKeyValueStore;
import com.evolution.dropfiledaemon.tunnel.CryptoTunnel;
import com.evolution.dropfiledaemon.tunnel.SecureEnvelope;
import com.evolution.dropfiledaemon.util.AccessKeyUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.net.URI;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
@Component
public class ApiHandshakeFacade {

    private final ApplicationConfigStore applicationConfigStore;

    private final HandshakeClient handshakeClient;

    private final CryptoTunnel cryptoTunnel;

    private final ObjectMapper objectMapper;

    @SneakyThrows
    public ApiHandshakeStatusResponseDTO handshake(ApiHandshakeRequestDTO requestDTO) {
        String secret = new String(CommonUtils.decodeBase64(requestDTO.key()));

        HandshakeRequestDTO.Payload requestPayload = new HandshakeRequestDTO.Payload(
                applicationConfigStore.getKeysConfigStore().getRequired().rsa().publicKey(),
                applicationConfigStore.getKeysConfigStore().getRequired().dh().publicKey(),
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
                CryptoRSA.getPrivateKey(applicationConfigStore.getKeysConfigStore().getRequired().rsa().privateKey())
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

        HandshakeResponseDTO.Payload responsePayload = objectMapper.readValue(
                decryptResponsePayload,
                HandshakeResponseDTO.Payload.class
        );
        if (Math.abs(System.currentTimeMillis() - responsePayload.timestamp()) > 30_000) {
            throw new RuntimeException("Timed out");
        }

        boolean verify = CryptoRSA.verify(
                decryptResponsePayload,
                handshakeResponseDTO.signature(),
                CryptoRSA.getPublicKey(responsePayload.publicKeyRSA())
        );
        if (!verify) {
            throw new RuntimeException("Signature verification failed");
        }

        String fingerprint = CommonUtils.getFingerprint(responsePayload.publicKeyRSA());
        applicationConfigStore.getHandshakeStore().trustedOutStore().save(
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

    @SneakyThrows
    public ApiHandshakeStatusResponseDTO handshakeReconnect(ApiHandshakeReconnectRequestDTO requestDTO) {
        TrustedOutKeyValueStore.TrustedOutValue trustedOutValue = applicationConfigStore.getHandshakeStore().trustedOutStore()
                .getRequiredByAddressURI(CommonUtils.toURI(requestDTO.address()))
                .getValue();
        ApiHandshakeStatusResponseDTO apiHandshakeStatusResponseDTO = pingHandshake(trustedOutValue);
        applicationConfigStore.getHandshakeStore().trustedOutStore().save(
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
        TrustedOutKeyValueStore.TrustedOutValue trustedOutValue = applicationConfigStore.getHandshakeStore().trustedOutStore()
                .getRequiredLatestUpdated()
                .getValue();
        return pingHandshake(trustedOutValue);
    }

    @SneakyThrows
    private ApiHandshakeStatusResponseDTO pingHandshake(TrustedOutKeyValueStore.TrustedOutValue trustedOutValue) {
        byte[] secret = CryptoECDH.getSecretKey(
                CryptoECDH.getPrivateKey(applicationConfigStore.getKeysConfigStore().getRequired().dh().privateKey()),
                CryptoECDH.getPublicKey(trustedOutValue.publicKeyDH())
        );
        SecretKey secretKey = cryptoTunnel.secretKey(secret);

        HandshakeRequestDTO.Payload requestPayload = new HandshakeRequestDTO.Payload(
                applicationConfigStore.getKeysConfigStore().getRequired().rsa().publicKey(),
                applicationConfigStore.getKeysConfigStore().getRequired().dh().publicKey(),
                System.currentTimeMillis()
        );
        byte[] requestPayloadByteArray = objectMapper.writeValueAsBytes(requestPayload);
        SecureEnvelope secureEnvelope = cryptoTunnel.encrypt(
                requestPayloadByteArray,
                secretKey
        );

        byte[] signature = CryptoRSA.sign(
                requestPayloadByteArray,
                CryptoRSA.getPrivateKey(applicationConfigStore.getKeysConfigStore().getRequired().rsa().privateKey())
        );
        HandshakeRequestDTO handshakeRequestDTO = new HandshakeRequestDTO(
                CommonUtils.getFingerprint(applicationConfigStore.getKeysConfigStore().getRequired().rsa().publicKey()),
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
        HandshakeResponseDTO.Payload responsePayload = objectMapper.readValue(
                decryptResponsePayload,
                HandshakeResponseDTO.Payload.class
        );
        if (Math.abs(System.currentTimeMillis() - responsePayload.timestamp()) > 30_000) {
            throw new RuntimeException("Timed out");
        }

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

        return new ApiHandshakeStatusResponseDTO(
                fingerprintResponse,
                addressURI.toString(),
                "Online",
                responsePayload.tunnelAlgorithm()
        );
    }

    public List<HandshakeApiTrustInResponseDTO> getTrustIt() {
        return applicationConfigStore.getHandshakeStore()
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
        return applicationConfigStore.getHandshakeStore()
                .trustedOutStore()
                .getAll()
                .entrySet()
                .stream()
                .map(it -> toHandshakeApiTrustOutResponseDTO(it))
                .toList();
    }

    public HandshakeApiTrustOutResponseDTO getLatestTrustOut() {
        Map.Entry<String, TrustedOutKeyValueStore.TrustedOutValue> trustedOutEntry = applicationConfigStore.getHandshakeStore().trustedOutStore()
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

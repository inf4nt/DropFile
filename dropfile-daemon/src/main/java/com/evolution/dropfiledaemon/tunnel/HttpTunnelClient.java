package com.evolution.dropfiledaemon.tunnel;

import com.evolution.dropfile.common.crypto.CryptoECDH;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.common.crypto.CryptoUtils;
import com.evolution.dropfile.common.crypto.SecureEnvelope;
import com.evolution.dropfile.configuration.keys.KeysConfigStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeStore;
import com.evolution.dropfiledaemon.handshake.store.TrustedOutKeyValueStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Component
public class HttpTunnelClient implements TunnelClient {

    private final HandshakeStore handshakeStore;

    private final KeysConfigStore keysConfigStore;

    private final CryptoTunnel cryptoTunnel;

    private final HttpClient httpClient;

    private final ObjectMapper objectMapper;

    @Autowired
    public HttpTunnelClient(HandshakeStore handshakeStore,
                            KeysConfigStore keysConfigStore,
                            CryptoTunnel cryptoTunnel,
                            HttpClient httpClient,
                            ObjectMapper objectMapper) {
        this.handshakeStore = handshakeStore;
        this.keysConfigStore = keysConfigStore;
        this.cryptoTunnel = cryptoTunnel;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @SneakyThrows
    @Override
    public <T> T send(Request request, Class<T> responseType) {
        try {
            TunnelRequestDTO.TunnelRequestPayload requestPayload = new TunnelRequestDTO.TunnelRequestPayload(
                    request.getAction(),
                    request.getPayload() == null ? null : objectMapper.writeValueAsBytes(request.getPayload()),
                    System.currentTimeMillis()
            );

            SecretKey secretKey = getSecretKey(CryptoUtils.decodeBase64(request.getPublicKeyDH()));

            SecureEnvelope secureEnvelope = cryptoTunnel.encrypt(objectMapper.writeValueAsBytes(requestPayload), secretKey);

            TunnelRequestDTO tunnelRequestDTO = new TunnelRequestDTO(
                    CryptoUtils.getFingerprint(keysConfigStore.getRequired().rsa().publicKey()),
                    CryptoUtils.encodeBase64(secureEnvelope.payload()),
                    CryptoUtils.encodeBase64(secureEnvelope.nonce())
            );

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(request.getAddress().resolve("/tunnel"))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(
                            objectMapper.writeValueAsBytes(tunnelRequestDTO))
                    )
                    .header("Content-Type", "application/json")
                    .build();

            HttpResponse<byte[]> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());

            if (httpResponse.statusCode() != 200) {
                return null;
            }

            TunnelResponseDTO tunnelResponseDTO = objectMapper.readValue(httpResponse.body(), TunnelResponseDTO.class);
            byte[] decryptPayload = cryptoTunnel.decrypt(
                    CryptoUtils.decodeBase64(tunnelResponseDTO.payload()),
                    CryptoUtils.decodeBase64(tunnelResponseDTO.nonce()),
                    secretKey
            );
            if (String.class.equals(responseType)) {
                return (T) new String(decryptPayload, StandardCharsets.UTF_8);
            }

            return objectMapper.readValue(decryptPayload, responseType);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @SneakyThrows
    @Override
    public <T> T send(RequestTrusted request, Class<T> responseType) {
        String fingerprint = request.fingerprint();
        TrustedOutKeyValueStore.TrustedOutValue trustedOutValue = handshakeStore.trustedOutStore()
                .get(fingerprint)
                .orElse(null);
        if (trustedOutValue == null) {
            throw new RuntimeException("No trusted-out connection found: " + fingerprint);
        }

        return send(new Request() {
                        @Override
                        public URI getAddress() {
                            return trustedOutValue.addressURI();
                        }

                        @Override
                        public String getAction() {
                            return request.getAction();
                        }

                        @Override
                        public Object getPayload() {
                            return request.getPayload();
                        }

                        @Override
                        public String getPublicKeyDH() {
                            return CryptoUtils.encodeBase64(trustedOutValue.publicKeyDH());
                        }
                    },
                responseType
        );

//        String fingerprint = request.fingerprint();
//        TrustedOutKeyValueStore.TrustedOutValue trustedOutValue = handshakeStore.trustedOutStore()
//                .get(fingerprint)
//                .orElse(null);
//        if (trustedOutValue == null) {
//            throw new RuntimeException("No trusted-out connection found: " + fingerprint);
//        }
//
//        TunnelRequestDTO.TunnelRequestPayload requestPayload = new TunnelRequestDTO.TunnelRequestPayload(
//                request.getAction(),
//                objectMapper.writeValueAsBytes(request.getPayload()),
//                System.currentTimeMillis()
//        );
//
//        SecretKey secretKey = getSecretKey(trustedOutValue);
//
//        SecureEnvelope secureEnvelope = cryptoTunnel.encrypt(objectMapper.writeValueAsBytes(requestPayload), secretKey);
//
//        TunnelRequestDTO tunnelRequestDTO = new TunnelRequestDTO(
//                CryptoUtils.getFingerprint(keysConfigStore.getRequired().rsa().publicKey()),
//                CryptoUtils.encodeBase64(secureEnvelope.payload()),
//                CryptoUtils.encodeBase64(secureEnvelope.nonce())
//        );
//
//        HttpRequest httpRequest = HttpRequest.newBuilder()
//                .uri(trustedOutValue.addressURI().resolve("/tunnel"))
//                .POST(HttpRequest.BodyPublishers.ofByteArray(
//                        objectMapper.writeValueAsBytes(tunnelRequestDTO))
//                )
//                .header("Content-Type", "application/json")
//                .build();
//
//        HttpResponse<byte[]> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
//
//        if (httpResponse.statusCode() != 200) {
//            throw new RuntimeException("Failed : HTTP error code : " + httpResponse.statusCode());
//        }
//
//        return objectMapper.readValue(httpResponse.body(), responseType);
    }

    private SecretKey getSecretKey(TrustedOutKeyValueStore.TrustedOutValue trustedOutValue) {
        return getSecretKey(trustedOutValue.publicKeyDH());
    }

    private SecretKey getSecretKey(byte[] publicKeyDH) {
        byte[] secret = CryptoECDH.getSecretKey(
                CryptoECDH.getPrivateKey(keysConfigStore.getRequired().dh().privateKey()),
                CryptoECDH.getPublicKey(publicKeyDH)
        );
        return cryptoTunnel.secretKey(secret);
    }
}

package com.evolution.dropfiledaemon.tunnel.service;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.crypto.CryptoECDH;
import com.evolution.dropfiledaemon.tunnel.CryptoTunnel;
import com.evolution.dropfiledaemon.tunnel.SecureEnvelope;
import com.evolution.dropfile.store.keys.KeysConfigStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeStore;
import com.evolution.dropfiledaemon.handshake.store.TrustedOutKeyValueStore;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelClient;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelRequestDTO;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelResponseDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.lang.reflect.Type;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;

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
        return send(request, new TypeReference<T>() {
            @Override
            public Type getType() {
                return responseType;
            }
        });
    }

    @Override
    public <T> T send(Request request, TypeReference<T> responseType) {
        try {
            TrustedOutKeyValueStore.TrustedOutValue trustedOutValue = getTrustedOutValue(request);

            SecretKey secretKey = getSecretKey(trustedOutValue.publicKeyDH());

            SecureEnvelope secureEnvelope = encrypt(request, secretKey);

            TunnelRequestDTO tunnelRequestDTO = new TunnelRequestDTO(
                    CommonUtils.getFingerprint(keysConfigStore.getRequired().rsa().publicKey()),
                    secureEnvelope.payload(),
                    secureEnvelope.nonce()
            );

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(trustedOutValue.addressURI().resolve("/tunnel"))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(
                            objectMapper.writeValueAsBytes(tunnelRequestDTO))
                    )
                    .header("Content-Type", "application/json")
                    .build();

            HttpResponse<byte[]> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());

            if (httpResponse.statusCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + httpResponse.statusCode());
            }

            TunnelResponseDTO tunnelResponseDTO = objectMapper.readValue(httpResponse.body(), TunnelResponseDTO.class);
            byte[] decryptPayload = cryptoTunnel.decrypt(
                    tunnelResponseDTO.payload(),
                    tunnelResponseDTO.nonce(),
                    secretKey
            );
            if (String.class.equals(responseType.getType())) {
                return (T) new String(decryptPayload, StandardCharsets.UTF_8);
            }
            return objectMapper.readValue(decryptPayload, responseType);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private SecureEnvelope encrypt(Request request, SecretKey secretKey) throws JsonProcessingException {
        byte[] payload = switch (request.getBody()) {
            case null -> null;
            case String string -> string.getBytes(StandardCharsets.UTF_8);
            case byte[] byteArray -> byteArray;
            default -> objectMapper.writeValueAsBytes(request.getBody());
        };

        return cryptoTunnel.encrypt(
                objectMapper.writeValueAsBytes(
                        new TunnelRequestDTO.TunnelRequestPayload(
                                request.getAction(),
                                payload,
                                System.currentTimeMillis()
                        )
                ),
                secretKey
        );
    }

    private TrustedOutKeyValueStore.TrustedOutValue getTrustedOutValue(Request request) {
        if (request.getFingerprint() == null) {
            return getLatestConnection();
        }
        return handshakeStore
                .trustedOutStore()
                .get(request.getFingerprint())
                .map(it -> it.getValue())
                .orElseThrow(() -> new RuntimeException("No trusted-out found: " + request.getFingerprint()));
    }

    private SecretKey getSecretKey(byte[] publicKeyDH) {
        byte[] secret = CryptoECDH.getSecretKey(
                CryptoECDH.getPrivateKey(keysConfigStore.getRequired().dh().privateKey()),
                CryptoECDH.getPublicKey(publicKeyDH)
        );
        return cryptoTunnel.secretKey(secret);
    }

    private TrustedOutKeyValueStore.TrustedOutValue getLatestConnection() {
        return handshakeStore.trustedOutStore().getAll().values()
                .stream()
                .max(Comparator.comparing(TrustedOutKeyValueStore.TrustedOutValue::updated))
                .orElseThrow(() -> new RuntimeException("No trusted-out connections found"));
    }
}

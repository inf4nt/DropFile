package com.evolution.dropfiledaemon.tunnel;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.crypto.CryptoECDH;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.common.crypto.SecureEnvelope;
import com.evolution.dropfile.configuration.keys.KeysConfigStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeStore;
import com.evolution.dropfiledaemon.handshake.store.TrustedOutKeyValueStore;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelClient;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelRequestDTO;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
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
        try {
            TrustedOutKeyValueStore.TrustedOutValue trustedOutValue = getTrustedOutValue(request);

            SecretKey secretKey = getSecretKey(trustedOutValue.publicKeyDH());

            SecureEnvelope secureEnvelope = cryptoTunnel.encrypt(
                    objectMapper.writeValueAsBytes(
                            new TunnelRequestDTO.TunnelRequestPayload(
                                    request.action(),
                                    request.payload() == null ? null : objectMapper.writeValueAsBytes(request.payload()),
                                    System.currentTimeMillis()
                            )
                    ),
                    secretKey
            );

            TunnelRequestDTO tunnelRequestDTO = new TunnelRequestDTO(
                    CommonUtils.getFingerprint(keysConfigStore.getRequired().dh().publicKey()),
                    CommonUtils.encodeBase64(secureEnvelope.payload()),
                    CommonUtils.encodeBase64(secureEnvelope.nonce())
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
                    CommonUtils.decodeBase64(tunnelResponseDTO.payload()),
                    CommonUtils.decodeBase64(tunnelResponseDTO.nonce()),
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

    private TrustedOutKeyValueStore.TrustedOutValue getTrustedOutValue(Request request) {
        if (request.fingerprint() == null) {
            return getLatestConnection();
        }
        return handshakeStore
                .trustedOutStore()
                .get(request.fingerprint())
                .map(it -> it.getValue())
                .orElseThrow(() -> new RuntimeException("No trusted-out found: " + request.fingerprint()));
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

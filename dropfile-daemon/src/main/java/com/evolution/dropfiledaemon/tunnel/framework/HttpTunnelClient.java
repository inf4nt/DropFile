package com.evolution.dropfiledaemon.tunnel.framework;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.crypto.CryptoECDH;
import com.evolution.dropfiledaemon.configuration.ApplicationConfigStore;
import com.evolution.dropfiledaemon.handshake.store.TrustedOutKeyValueStore;
import com.evolution.dropfiledaemon.tunnel.CryptoTunnel;
import com.evolution.dropfiledaemon.tunnel.SecureEnvelope;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Comparator;

@RequiredArgsConstructor
@Component
public class HttpTunnelClient implements TunnelClient {

    private static final Duration HTTP_REQUEST_TIMEOUT = Duration.ofSeconds(60);

    private final ApplicationConfigStore applicationConfigStore;

    private final CryptoTunnel cryptoTunnel;

    private final HttpClient httpClient;

    private final ObjectMapper objectMapper;

    @SneakyThrows
    @Override
    public <T> T send(Request request, TypeReference<T> responseType) {
        HttpResponse<InputStream> httpResponse = null;
        try {
            TrustedOutKeyValueStore.TrustedOutValue trustedOutValue = getTrustedOutValue(request);

            SecretKey secretKey = getSecretKey(trustedOutValue.publicKeyDH());

            SecureEnvelope secureEnvelope = encrypt(request, secretKey);

            TunnelRequestDTO tunnelRequestDTO = new TunnelRequestDTO(
                    CommonUtils.getFingerprint(applicationConfigStore.getKeysConfigStore().getRequired().rsa().publicKey()),
                    secureEnvelope.payload(),
                    secureEnvelope.nonce()
            );

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(trustedOutValue.addressURI().resolve("/tunnel"))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(
                            objectMapper.writeValueAsBytes(tunnelRequestDTO))
                    )
                    .header("Content-Type", "application/json")
                    .timeout(HTTP_REQUEST_TIMEOUT)
                    .build();

            httpResponse = httpClient
                    .send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
            if (httpResponse.statusCode() != 200) {
                throw new RuntimeException("Unexpected tunnel http response status code. Expected: 200, actual: " + httpResponse.statusCode());
            }

            if (isInputStream(responseType)) {
                return (T) cryptoTunnel.decrypt(httpResponse.body(), secretKey);
            }

            try (InputStream decryptInputStream = cryptoTunnel.decrypt(httpResponse.body(), secretKey)) {
                if (responseType.getType().equals(String.class)) {
                    return (T) new String(decryptInputStream.readAllBytes(), StandardCharsets.UTF_8);
                }
                return objectMapper.readValue(decryptInputStream, responseType);
            }
        } catch (Exception e) {
            if (httpResponse != null) {
                try {
                    httpResponse.body().close();
                } catch (Exception closeException) {
                    closeException.printStackTrace();
                }
            }
            throw e;
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
                                request.getCommand(),
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
        return applicationConfigStore.getHandshakeStore()
                .trustedOutStore()
                .getRequired(request.getFingerprint())
                .getValue();
    }

    private SecretKey getSecretKey(byte[] publicKeyDH) {
        byte[] secret = CryptoECDH.getSecretKey(
                CryptoECDH.getPrivateKey(applicationConfigStore.getKeysConfigStore().getRequired().dh().privateKey()),
                CryptoECDH.getPublicKey(publicKeyDH)
        );
        return cryptoTunnel.secretKey(secret);
    }

    private TrustedOutKeyValueStore.TrustedOutValue getLatestConnection() {
        return applicationConfigStore.getHandshakeStore().trustedOutStore().getAll().values()
                .stream()
                .max(Comparator.comparing(TrustedOutKeyValueStore.TrustedOutValue::updated))
                .orElseThrow(() -> new RuntimeException("No trusted-out connections found"));
    }

    private boolean isInputStream(TypeReference<?> reference) {
        Type type = reference.getType();
        return type instanceof Class<?> clazz && clazz.isAssignableFrom(InputStream.class);
    }
}

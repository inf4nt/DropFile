package com.evolution.dropfiledaemon.tunnel.framework;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.crypto.CryptoECDH;
import com.evolution.dropfiledaemon.configuration.ApplicationConfigStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeSessionStore;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedOutStore;
import com.evolution.dropfiledaemon.tunnel.CryptoTunnel;
import com.evolution.dropfiledaemon.tunnel.SecureEnvelope;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.crypto.SecretKey;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

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
            Map.Entry<String, HandshakeSessionStore.SessionValue> sessionEntry = getSession(request);

            HandshakeSessionStore.SessionValue session = sessionEntry.getValue();
            String fingerprint = sessionEntry.getKey();

            SecretKey secretKey = getSecretKey(session);

            SecureEnvelope secureEnvelope = encrypt(request, secretKey);

            HandshakeTrustedOutStore.TrustedOut trustedOut = getTrustedOut(fingerprint);

            TunnelRequestDTO tunnelRequestDTO = new TunnelRequestDTO(
                    CommonUtils.getFingerprint(trustedOut.publicRSA()),
                    secureEnvelope.payload(),
                    secureEnvelope.nonce()
            );

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(trustedOut.addressURI().resolve("/tunnel"))
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

    private Map.Entry<String, HandshakeSessionStore.SessionValue> getSession(Request request) {
        if (ObjectUtils.isEmpty(request.getFingerprint())) {
            return applicationConfigStore.getHandshakeContextStore().sessionStore()
                    .getRequiredLatestUpdated();
        }
        return applicationConfigStore.getHandshakeContextStore().sessionStore()
                .getRequired(request.getFingerprint());
    }

    private SecretKey getSecretKey(HandshakeSessionStore.SessionValue session) {
        byte[] secret = CryptoECDH.getSecretKey(
                CryptoECDH.getPrivateKey(session.privateDH()),
                CryptoECDH.getPublicKey(session.remotePublicDH())
        );
        return cryptoTunnel.secretKey(secret);
    }

    private HandshakeTrustedOutStore.TrustedOut getTrustedOut(String fingerprint) {
        return applicationConfigStore.getHandshakeContextStore()
                .trustedOutStore().getRequired(fingerprint)
                .getValue();
    }

    private boolean isInputStream(TypeReference<?> reference) {
        Type type = reference.getType();
        return type instanceof Class<?> clazz && clazz.isAssignableFrom(InputStream.class);
    }
}

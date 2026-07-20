package com.evolution.dropfiledaemon.tunnel.framework;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.WatchdogInputStream;
import com.evolution.dropfile.common.crypto.CryptoECDH;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.common.crypto.SecureEnvelope;
import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedOutStore;
import com.evolution.dropfiledaemon.tunnel.TunnelRestController;
import com.evolution.dropfiledaemon.tunnel.framework.compress.CompressTunnelService;
import com.evolution.dropfiledaemon.tunnel.framework.monitor.TunnelTrafficMonitor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@RequiredArgsConstructor
@Component
public class HttpTunnelClient implements TunnelClient {

    private final DaemonApplicationProperties daemonApplicationProperties;

    private final CryptoTunnel cryptoTunnel;

    private final CompressTunnelService compressTunnelService;

    private final TunnelTrafficMonitor tunnelTrafficMonitor;

    private final HttpClient httpClient;

    private final ObjectMapper objectMapper;

    private final HandshakeTrustedOutStore handshakeTrustedOutStore;

    @Override
    public InputStream stream(Request request) {
        HttpResponse<InputStream> httpResponse = null;
        try {
            String fingerprint = request.getFingerprint();

            HandshakeTrustedOutStore.TrustedOut trustedOut = getTrustedOut(fingerprint);

            SecretKey secretKey = getSecretKey(trustedOut);

            SecureEnvelope secureEnvelope = encrypt(request, secretKey);

            TunnelRequestDTO tunnelRequestDTO = new TunnelRequestDTO(
                    CommonUtils.getFingerprint(trustedOut.handshake().publicRSA()),
                    secureEnvelope.payload(),
                    secureEnvelope.nonce()
            );

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(trustedOut.addressURI().resolve(TunnelRestController.TUNNEL_ENDPOINT))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(
                            objectMapper.writeValueAsBytes(tunnelRequestDTO))
                    )
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofMillis(daemonApplicationProperties.daemonTunnelClientHttpRequestTimeoutMillis))
                    .build();

            httpResponse = httpClient
                    .send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
            if (httpResponse.statusCode() != 200) {
                throw new RuntimeException("Unexpected tunnel http response status code. Expected: 200, actual: " + httpResponse.statusCode());
            }

            return getInputStreamResponse(httpResponse, fingerprint, secretKey);
        } catch (Exception e) {
            if (httpResponse != null) {
                try {
                    httpResponse.body().close();
                } catch (Exception closeException) {
                    closeException.printStackTrace();
                }
            }
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private InputStream getInputStreamResponse(HttpResponse<InputStream> httpResponse,
                                               String fingerprint,
                                               SecretKey secretKey) throws IOException {
        WatchdogInputStream watchdogInputStream = new WatchdogInputStream(
                httpResponse.body(),
                daemonApplicationProperties.daemonTunnelClientStreamMaxSize,
                Duration.ofMillis(daemonApplicationProperties.daemonTunnelClientStreamDeadlineTimeoutMillis)
        );
        InputStream tunnelTrafficMonitorInputStream = tunnelTrafficMonitor.inputStreamWrapper(fingerprint, watchdogInputStream);
        InputStream decrypt = cryptoTunnel.decrypt(
                tunnelTrafficMonitorInputStream,
                secretKey
        );
        if (daemonApplicationProperties.daemonTunnelClientCompressEnabled) {
            return compressTunnelService.decompress(decrypt);
        }
        return decrypt;
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
                                new TunnelRequestDTO.TunnelRequestConfiguration(
                                        daemonApplicationProperties.daemonTunnelClientCompressEnabled
                                ),
                                System.currentTimeMillis()
                        )
                ),
                secretKey
        );
    }

    private SecretKey getSecretKey(HandshakeTrustedOutStore.TrustedOut trustedOut) {
        byte[] secret = CryptoECDH.getSecretKey(
                CryptoECDH.getPrivateKey(trustedOut.session().privateDH()),
                CryptoECDH.getPublicKey(trustedOut.session().remotePublicDH())
        );
        return cryptoTunnel.secretKey(secret);
    }

    private HandshakeTrustedOutStore.TrustedOut getTrustedOut(String fingerprint) {
        return handshakeTrustedOutStore.getRequired(fingerprint)
                .getValue();
    }

    private boolean isInputStream(TypeReference<?> reference) {
        Type type = reference.getType();
        return type instanceof Class<?> clazz && clazz.isAssignableFrom(InputStream.class);
    }
}

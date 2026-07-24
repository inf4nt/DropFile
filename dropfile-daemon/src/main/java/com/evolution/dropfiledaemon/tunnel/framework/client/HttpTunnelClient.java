package com.evolution.dropfiledaemon.tunnel.framework.client;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.WatchdogInputStream;
import com.evolution.dropfile.common.crypto.CryptoECDH;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.common.crypto.SecureEnvelope;
import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedOutStore;
import com.evolution.dropfiledaemon.tunnel.TunnelServerRestController;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelClient;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelRequestDTO;
import com.evolution.dropfiledaemon.tunnel.framework.monitor.TunnelTrafficMonitor;
import com.evolution.dropfiledaemon.tunnel.framework.server.compress.CompressTunnelService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Slf4j
@RequiredArgsConstructor
@Component
public class HttpTunnelClient implements TunnelClient {

    private final DaemonApplicationProperties daemonApplicationProperties;

    private final CryptoTunnel cryptoTunnel;

    private final HttpClient httpClient;

    private final HandshakeTrustedOutStore handshakeTrustedOutStore;

    private final TunnelTrafficMonitor tunnelTrafficMonitor;

    private final CompressTunnelService compressTunnelService;

    private final ObjectMapper objectMapper;

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
                    .uri(trustedOut.addressURI().resolve(TunnelServerRestController.TUNNEL_ENDPOINT))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(
                            objectMapper.writeValueAsBytes(tunnelRequestDTO))
                    )
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofMillis(daemonApplicationProperties.daemonTunnelClientHttpRequestTimeoutMillis))
                    .build();

            httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());

            if (httpResponse.statusCode() != 200) {
                throw new RuntimeException(
                        "Unexpected tunnel HTTP response status code. Expected: 200, actual: " + httpResponse.statusCode()
                );
            }

            return getInputStreamResponse(httpResponse.body(), fingerprint, secretKey);
        } catch (Exception e) {
            if (httpResponse != null) {
                try {
                    httpResponse.body().close();
                } catch (Exception closeException) {
                    log.error("Failed to close HTTP response body stream during failure cleanup", closeException);
                }
            }
            throw new RuntimeException("Tunnel streaming request failed: " + e.getMessage(), e);
        }
    }

    private InputStream getInputStreamResponse(InputStream inputStream,
                                               String fingerprint,
                                               SecretKey secretKey) throws IOException {
        WatchdogInputStream watchdogInputStream = new WatchdogInputStream(
                inputStream,
                daemonApplicationProperties.daemonTunnelClientStreamMaxSize,
                Duration.ofMillis(daemonApplicationProperties.daemonTunnelClientStreamDeadlineTimeoutMillis)
        );
        InputStream trafficMonitorInputStream = tunnelTrafficMonitor.inputStreamWrapper(fingerprint, watchdogInputStream);
        InputStream decryptedStream = cryptoTunnel.decrypt(trafficMonitorInputStream, secretKey);

        if (daemonApplicationProperties.daemonTunnelClientCompressEnabled) {
            return compressTunnelService.decompress(decryptedStream);
        }
        return decryptedStream;
    }

    private SecureEnvelope encrypt(Request request, SecretKey secretKey) throws JsonProcessingException {
        byte[] payload = switch (request.getBody()) {
            case null -> null;
            case String string -> string.getBytes(StandardCharsets.UTF_8);
            case byte[] byteArray -> byteArray;
            default -> objectMapper.writeValueAsBytes(request.getBody());
        };

        String requestId = CommonUtils.generateRawSecretNonce12();

        return cryptoTunnel.encrypt(
                objectMapper.writeValueAsBytes(
                        new TunnelRequestDTO.Payload(
                                requestId,
                                request.getCommand(),
                                payload,
                                new TunnelRequestDTO.Configuration(
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
        return handshakeTrustedOutStore.getRequired(fingerprint).getValue();
    }
}

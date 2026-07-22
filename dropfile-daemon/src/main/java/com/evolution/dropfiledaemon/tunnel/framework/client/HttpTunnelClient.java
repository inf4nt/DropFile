package com.evolution.dropfiledaemon.tunnel.framework.client;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.crypto.CryptoECDH;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfile.common.crypto.SecureEnvelope;
import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedOutStore;
import com.evolution.dropfiledaemon.tunnel.TunnelServerRestController;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelRequestDTO;
import com.evolution.dropfiledaemon.tunnel.framework.exception.TunnelClientException;
import com.evolution.dropfiledaemon.tunnel.framework.client.handler.TunnelClientHandler;
import com.evolution.dropfiledaemon.tunnel.framework.client.handler.TunnelClientHandlerProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.io.InputStream;
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

    private final HttpClient httpClient;

    private final ObjectMapper objectMapper;

    private final HandshakeTrustedOutStore handshakeTrustedOutStore;

    private final TunnelClientHandlerProvider tunnelClientHandlerProvider;

    @Override
    public InputStream stream(Request request) throws TunnelClientException {
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

            httpResponse = httpClient
                    .send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
            if (httpResponse.statusCode() != 200) {
                throw new IllegalStateException("Unexpected tunnel http response status code. Expected: 200, actual: " + httpResponse.statusCode());
            }

            InputStream httpResponseInputStream = httpResponse.body();
            int markerStatusCode = httpResponseInputStream.read();
            TunnelClientHandler tunnelClientHandler = tunnelClientHandlerProvider.getHandler(markerStatusCode);
            return tunnelClientHandler.handle(fingerprint, trustedOut, secretKey, httpResponseInputStream);
        } catch (Exception e) {
            if (httpResponse != null) {
                try {
                    httpResponse.body().close();
                } catch (Exception closeException) {
                    closeException.printStackTrace();
                }
            }
            throw new TunnelClientException(e.getMessage(), e);
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
}

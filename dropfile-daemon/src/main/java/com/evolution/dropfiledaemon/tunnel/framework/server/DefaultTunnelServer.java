package com.evolution.dropfiledaemon.tunnel.framework.server;

import com.evolution.dropfile.common.crypto.CryptoECDH;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedInStore;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelRequestDTO;
import com.evolution.dropfiledaemon.tunnel.framework.server.chain.TunnelServerChainProcedureContext;
import com.evolution.dropfiledaemon.tunnel.framework.server.chain.TunnelServerChainProcedureProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.OutputStream;

@RequiredArgsConstructor
@Component
public class DefaultTunnelServer implements TunnelServer {

    private final CryptoTunnel cryptoTunnel;

    private final HandshakeTrustedInStore handshakeTrustedInStore;

    private final ObjectMapper objectMapper;

    private final TunnelServerChainFactory tunnelServerChainFactory;

    @Override
    public void dispatchStream(TunnelRequestDTO requestDTO, OutputStream outputStream) throws IOException {
        String fingerprint = requestDTO.fingerprint();
        SecretKey secretKey = getSecretKey(fingerprint);

        TunnelRequestDTO.TunnelRequestPayload tunnelRequestPayload = decrypt(requestDTO, secretKey);

        TunnelServerChainProcedureProcessor processor = tunnelServerChainFactory.createProcessor();
        processor.doChain(new TunnelServerChainProcedureContext(
                fingerprint,
                tunnelRequestPayload,
                secretKey,
                outputStream
        ));
        outputStream.flush();
    }

    private SecretKey getSecretKey(String fingerprint) {
        HandshakeTrustedInStore.TrustedIn trustedIn = handshakeTrustedInStore.getRequired(fingerprint)
                .getValue();
        byte[] secret = CryptoECDH.getSecretKey(
                CryptoECDH.getPrivateKey(trustedIn.session().privateDH()),
                CryptoECDH.getPublicKey(trustedIn.session().remotePublicDH())
        );
        return cryptoTunnel.secretKey(secret);
    }

    @SneakyThrows
    private TunnelRequestDTO.TunnelRequestPayload decrypt(TunnelRequestDTO requestDTO, SecretKey secretKey) {
        byte[] decrypt = cryptoTunnel.decrypt(
                requestDTO.payload(),
                requestDTO.nonce(),
                secretKey
        );
        return objectMapper.readValue(decrypt, TunnelRequestDTO.TunnelRequestPayload.class);
    }
}


//@RequiredArgsConstructor
//@Component
//public class DefaultTunnelDispatcher implements TunnelDispatcher {
//
//    private final DaemonApplicationProperties daemonApplicationProperties;
//
//    private final CommandHandlerExecutor commandHandlerExecutor;
//
//    private final CryptoTunnel cryptoTunnel;
//
//    private final CompressTunnelService compressTunnelService;
//
//    private final TunnelTrafficMonitor tunnelTrafficMonitor;
//
//    private final ObjectMapper objectMapper;
//
//    private final HandshakeTrustedInStore handshakeTrustedInStore;
//
//    private final List<TunnelDispatcherHandlerChain> handlerChains;
//
//    @Override
//    public void dispatchStream(TunnelRequestDTO requestDTO, OutputStream outputStream) throws IOException {
//        String fingerprint = requestDTO.fingerprint();
//        SecretKey secretKey = getSecretKey(fingerprint);
//
//        TunnelRequestDTO.TunnelRequestPayload tunnelRequestPayload = decrypt(requestDTO, secretKey);
//
//        validatePayloadTimestamp(tunnelRequestPayload);
//
//        for (TunnelDispatcherHandlerChain handlerChain : handlerChains) {
//            boolean apply = handlerChain.apply(fingerprint);
//            if (!apply) {
//                handlerChain.execute(fingerprint, secretKey, tunnelRequestPayload, outputStream);
//            }
//        }
//
//        if (isHandshakeExpired(fingerprint)) {
//            handshakeExpiredHandler(fingerprint, outputStream);
//        } else if (isSessionExpired(fingerprint)) {
//            sessionExpiredHandler(fingerprint, outputStream);
//        } else {
//            commonHandler(fingerprint, outputStream, tunnelRequestPayload, secretKey);
//        }
//    }
//
//    private void commonHandler(String fingerprint, OutputStream outputStream, TunnelRequestDTO.TunnelRequestPayload tunnelRequestPayload, SecretKey secretKey) throws IOException {
//        Object handlerResult = commandHandlerExecutor.handle(tunnelRequestPayload);
//
//        outputStream.write(TunnelDispatcherStatus.OK.getStatusCode());
//        try (InputStream inputStreamResult = handlerResultToInputStream(handlerResult);
//             OutputStream tunnelTrafficMonitorOutputStream = tunnelTrafficMonitor.outputStreamWrapper(
//                     fingerprint,
//                     new CloseShieldOutputStream(outputStream)
//             );
//             OutputStream encryptOutputStream = cryptoTunnel.encryptWrapper(tunnelTrafficMonitorOutputStream, secretKey);
//             OutputStream compressOutputStream = getCompressOutputStream(tunnelRequestPayload.configuration(), encryptOutputStream)) {
//            inputStreamResult.transferTo(compressOutputStream);
//            compressOutputStream.flush();
//        }
//    }
//
//    private void handshakeExpiredHandler(String fingerprint, OutputStream outputStream) throws IOException {
//        errorHandler(fingerprint, TunnelDispatcherStatus.HANDSHAKE_EXPIRED, outputStream);
//    }
//
//    private void sessionExpiredHandler(String fingerprint, OutputStream outputStream) throws IOException {
//        errorHandler(fingerprint, TunnelDispatcherStatus.SESSION_EXPIRED, outputStream);
//    }
//
//    private void errorHandler(String fingerprint, TunnelDispatcherStatus status, OutputStream outputStream) throws IOException {
//        HandshakeTrustedInStore.TrustedIn trustedIn = handshakeTrustedInStore.getRequired(fingerprint)
//                .getValue();
//
//        Instant now = Instant.now();
//
//        byte[] payload = ByteBuffer.allocate(Long.BYTES)
//                .putLong(now.toEpochMilli())
//                .array();
//        byte[] sign = CryptoRSA.sign(payload, CryptoRSA.getPrivateKey(trustedIn.handshake().privateRSA()));
//
//        outputStream.write(status.getStatusCode());
//        outputStream.flush();
//
//        outputStream.write(payload);
//        outputStream.write(sign);
//        outputStream.flush();
//    }
//
//    private OutputStream getCompressOutputStream(TunnelRequestDTO.TunnelRequestConfiguration configuration, OutputStream outputStream) throws IOException {
//        if (configuration.compress()) {
//            return compressTunnelService.compressWrapper(outputStream);
//        }
//        return outputStream;
//    }
//
//    @SneakyThrows
//    private InputStream handlerResultToInputStream(Object handlerResult) {
//        if (handlerResult instanceof InputStream inputStream) {
//            return inputStream;
//        }
//        if (handlerResult instanceof byte[] arrayResult) {
//            return new ByteArrayInputStream(arrayResult);
//        }
//        if (handlerResult instanceof String stringResult) {
//            return new ByteArrayInputStream(stringResult.getBytes());
//        }
//
//        byte[] bytes = objectMapper.writeValueAsBytes(handlerResult);
//        return new ByteArrayInputStream(bytes);
//    }
//
//    private SecretKey getSecretKey(String fingerprint) {
//        HandshakeTrustedInStore.TrustedIn trustedIn = handshakeTrustedInStore.getRequired(fingerprint)
//                .getValue();
//        byte[] secret = CryptoECDH.getSecretKey(
//                CryptoECDH.getPrivateKey(trustedIn.session().privateDH()),
//                CryptoECDH.getPublicKey(trustedIn.session().remotePublicDH())
//        );
//        return cryptoTunnel.secretKey(secret);
//    }
//
//    private boolean isHandshakeExpired(String fingerprint) {
//        HandshakeTrustedInStore.TrustedIn trustedIn = handshakeTrustedInStore.getRequired(fingerprint)
//                .getValue();
//        long maxTTL = 60_000;
//        Instant now = Instant.now();
//
//        return now.isAfter(trustedIn.created().plus(maxTTL, ChronoUnit.MILLIS));
//    }
//
//    private boolean isSessionExpired(String fingerprint) {
//        HandshakeTrustedInStore.TrustedIn trustedIn = handshakeTrustedInStore.getRequired(fingerprint)
//                .getValue();
//        long maxTTL = 10_000;
//        Instant now = Instant.now();
//
//        Instant sessionUpdated = Stream.of(trustedIn.sessionUpdatedByUser(), trustedIn.sessionUpdatedBySystem())
//                .filter(it -> it != null)
//                .max(Instant::compareTo)
//                .orElseThrow();
//        return now.isAfter(sessionUpdated.plus(maxTTL, ChronoUnit.MILLIS));
//    }
//
//    @SneakyThrows
//    private TunnelRequestDTO.TunnelRequestPayload decrypt(TunnelRequestDTO requestDTO, SecretKey secretKey) {
//        byte[] decrypt = cryptoTunnel.decrypt(
//                requestDTO.payload(),
//                requestDTO.nonce(),
//                secretKey
//        );
//        return objectMapper.readValue(decrypt, TunnelRequestDTO.TunnelRequestPayload.class);
//    }
//
//    private void validatePayloadTimestamp(TunnelRequestDTO.TunnelRequestPayload tunnelRequestPayload) {
//        long requestTime = Math.abs(System.currentTimeMillis() - tunnelRequestPayload.timestamp());
//        int tunnelServerPayloadLifeTime = daemonApplicationProperties.daemonTunnelServerPayloadLifeTime;
//        if (requestTime > tunnelServerPayloadLifeTime) {
//            throw new RuntimeException(
//                    String.format(
//                            "Tunnel request timeout exception. Expected %s actual %s",
//                            tunnelServerPayloadLifeTime, requestTime
//                    )
//            );
//        }
//    }
//}

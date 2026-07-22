package com.evolution.dropfiledaemon.tunnel.framework.server;

import com.evolution.dropfile.common.crypto.CryptoECDH;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedInStore;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelRequestDTO;
import com.evolution.dropfiledaemon.tunnel.framework.server.chain.TunnelDispatcherChainProcessor;
import com.evolution.dropfiledaemon.tunnel.framework.server.chain.TunnelDispatcherChainProcessorContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.OutputStream;

@RequiredArgsConstructor
@Component
public class DefaultTunnelDispatcher implements TunnelDispatcher {

    private final CryptoTunnel cryptoTunnel;

    private final HandshakeTrustedInStore handshakeTrustedInStore;

    private final ObjectMapper objectMapper;

    private final TunnelServerChainFactory tunnelServerChainFactory;

    @Override
    public void dispatchStream(TunnelRequestDTO requestDTO, OutputStream outputStream) throws IOException {
        String fingerprint = requestDTO.fingerprint();
        SecretKey secretKey = getSecretKey(fingerprint);

        TunnelRequestDTO.TunnelRequestPayload tunnelRequestPayload = decrypt(requestDTO, secretKey);

        TunnelDispatcherChainProcessor processor = tunnelServerChainFactory.createProcessor();
        processor.proceed(new TunnelDispatcherChainProcessorContext(
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

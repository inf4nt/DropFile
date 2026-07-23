package com.evolution.dropfiledaemon.tunnel.framework.server;

import com.evolution.dropfile.common.crypto.CryptoECDH;
import com.evolution.dropfile.common.crypto.CryptoTunnel;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedInStore;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelRequestDTO;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelServerDispatcher;
import com.evolution.dropfiledaemon.tunnel.framework.server.chain.TunnelServerDispatcherChainProcessor;
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
public class DefaultTunnelServerDispatcher implements TunnelServerDispatcher {

    private final CryptoTunnel cryptoTunnel;

    private final HandshakeTrustedInStore handshakeTrustedInStore;

    private final ObjectMapper objectMapper;

    private final TunnelServerDispatcherChainProcessorFactory tunnelServerDispatcherChainProcessorFactory;

    @Override
    public void dispatchStream(TunnelRequestDTO requestDTO, OutputStream outputStream) throws IOException {
        String fingerprint = requestDTO.fingerprint();
        SecretKey secretKey = getSecretKey(fingerprint);

        TunnelRequestDTO.Payload payload = decrypt(requestDTO, secretKey);

        TunnelServerDispatcherChainProcessor processor = tunnelServerDispatcherChainProcessorFactory.createProcessor();
        processor.proceed(new TunnelDispatcherChainProcessorContext(
                fingerprint,
                payload,
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
    private TunnelRequestDTO.Payload decrypt(TunnelRequestDTO requestDTO, SecretKey secretKey) {
        byte[] decrypt = cryptoTunnel.decrypt(
                requestDTO.payload(),
                requestDTO.nonce(),
                secretKey
        );
        return objectMapper.readValue(decrypt, TunnelRequestDTO.Payload.class);
    }
}

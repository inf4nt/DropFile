package com.evolution.dropfiledaemon.tunnel.framework.server.chain.procedure;

import com.evolution.dropfile.common.crypto.CryptoRSA;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedInStore;
import com.evolution.dropfiledaemon.tunnel.framework.TunnelServerDispatcherStatus;
import com.evolution.dropfiledaemon.tunnel.framework.server.chain.TunnelServerDispatcherChainProcessor;
import com.evolution.dropfiledaemon.tunnel.framework.server.chain.TunnelDispatcherChainProcessorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
@Component
public class HandshakeExpiredTunnelServerDispatcherChain implements TunnelServerDispatcherChain {

    private final HandshakeTrustedInStore handshakeTrustedInStore;

    @Override
    public void doChain(TunnelDispatcherChainProcessorContext ctx,
                        TunnelServerDispatcherChainProcessor processor) throws IOException {
        HandshakeTrustedInStore.TrustedIn trustedIn = handshakeTrustedInStore
                .getRequired(ctx.fingerprint())
                .getValue();

        Instant now = Instant.now();
        Instant expiredAt = trustedIn.created().plus(60, ChronoUnit.SECONDS);

        if (now.isAfter(expiredAt)) {
            try (OutputStream outputStream = ctx.outputStream()) {
                byte[] payload = ByteBuffer.allocate(Long.BYTES)
                        .putLong(now.toEpochMilli())
                        .array();
                byte[] sign = CryptoRSA.sign(payload, CryptoRSA.getPrivateKey(trustedIn.handshake().privateRSA()));

                outputStream.write(TunnelServerDispatcherStatus.HANDSHAKE_EXPIRED.getStatusCode());
                outputStream.flush();

                outputStream.write(payload);
                outputStream.write(sign);
                outputStream.flush();
            }
            return;
        }

        processor.proceed(ctx);
    }
}

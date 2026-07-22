package com.evolution.dropfiledaemon.tunnel.framework.server.chain.procedure;

import com.evolution.dropfile.common.crypto.CryptoRSA;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedInStore;
import com.evolution.dropfiledaemon.tunnel.framework.server.TunnelDispatcher;
import com.evolution.dropfiledaemon.tunnel.framework.server.chain.TunnelDispatcherChainProcessor;
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
import java.util.stream.Stream;

@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
@Component
public class SessionExpiredTunnelDispatcherChain
        implements TunnelDispatcherChain {

    private final HandshakeTrustedInStore handshakeTrustedInStore;

    @Override
    public void doChain(TunnelDispatcherChainProcessorContext ctx,
                        TunnelDispatcherChainProcessor processor) throws IOException {
        HandshakeTrustedInStore.TrustedIn trustedIn = handshakeTrustedInStore
                .getRequired(ctx.fingerprint())
                .getValue();

        Instant now = Instant.now();
        Instant expiredAt = Stream.of(trustedIn.sessionUpdatedByUser(), trustedIn.sessionUpdatedBySystem())
                .filter(it -> it != null)
                .max(Instant::compareTo)
                .orElseThrow()
                .plus(10, ChronoUnit.SECONDS);

        if (now.isAfter(expiredAt)) {
            try (OutputStream outputStream = ctx.outputStream()) {
                byte[] payload = ByteBuffer.allocate(Long.BYTES)
                        .putLong(now.toEpochMilli())
                        .array();
                byte[] sign = CryptoRSA.sign(payload, CryptoRSA.getPrivateKey(trustedIn.handshake().privateRSA()));

                outputStream.write(TunnelDispatcher.TunnelDispatcherStatus.SESSION_EXPIRED.getStatusCode());
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

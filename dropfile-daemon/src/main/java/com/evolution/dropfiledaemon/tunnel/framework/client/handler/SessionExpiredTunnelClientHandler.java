package com.evolution.dropfiledaemon.tunnel.framework.client.handler;

import com.evolution.dropfile.common.crypto.CryptoRSA;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedOutStore;
import com.evolution.dropfiledaemon.tunnel.framework.server.TunnelDispatcher;
import com.evolution.dropfiledaemon.tunnel.framework.client.exception.TunnelClientSessionExpiredException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

@RequiredArgsConstructor
@Component
public class SessionExpiredTunnelClientHandler implements TunnelClientHandler {

    private final HandshakeTrustedOutStore handshakeTrustedOutStore;

    @Override
    public int getStatusCode() {
        return TunnelDispatcher.TunnelDispatcherStatus.SESSION_EXPIRED.getStatusCode();
    }

    @Override
    public InputStream handle(String fingerprint,
                              HandshakeTrustedOutStore.TrustedOut trustedOut,
                              SecretKey secretKey,
                              InputStream inputStream) throws TunnelClientSessionExpiredException, IOException {
        // TODO payload must be timestamp + operation. Domain Separation

        byte[] payload = inputStream.readNBytes(Long.BYTES);
        byte[] signature = inputStream.readNBytes(256);
        CryptoRSA.verify(
                payload,
                signature,
                CryptoRSA.getPublicKey(trustedOut.handshake().remoteRSA())
        );

        long timestamp = ByteBuffer.wrap(payload).getLong();

        HandshakeTrustedOutStore.TrustedOut trustedOutCurrent = handshakeTrustedOutStore.getRequired(fingerprint).getValue();
        if (trustedOutCurrent.sessionRefreshRequestTimestamp() >= timestamp) {
            throw new IllegalArgumentException(
                    "Incoming timestamp (%d) must be greater than current stored timestamp (%d) for fingerprint: %s"
                            .formatted(timestamp, trustedOutCurrent.sessionRefreshRequestTimestamp(), fingerprint)
            );
        }

        throw new TunnelClientSessionExpiredException(fingerprint, timestamp);
    }
}

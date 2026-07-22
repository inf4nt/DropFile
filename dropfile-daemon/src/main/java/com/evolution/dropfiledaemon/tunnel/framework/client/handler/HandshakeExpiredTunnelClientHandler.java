package com.evolution.dropfiledaemon.tunnel.framework.client.handler;

import com.evolution.dropfile.common.crypto.CryptoRSA;
import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedOutStore;
import com.evolution.dropfiledaemon.tunnel.framework.server.TunnelDispatcher;
import com.evolution.dropfiledaemon.tunnel.framework.exception.TunnelClientHandshakeExpiredException;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

@Component
public class HandshakeExpiredTunnelClientHandler implements TunnelClientHandler {

    @Override
    public int getStatusCode() {
        return TunnelDispatcher.TunnelDispatcherStatus.HANDSHAKE_EXPIRED.getStatusCode();
    }

    @Override
    public InputStream handle(String fingerprint,
                              HandshakeTrustedOutStore.TrustedOut trustedOut,
                              SecretKey secretKey,
                              InputStream inputStream) throws TunnelClientHandshakeExpiredException, IOException {
        byte[] payload = inputStream.readNBytes(Long.BYTES);
        byte[] signature = inputStream.readNBytes(256);
        CryptoRSA.verify(payload, signature, CryptoRSA.getPublicKey(trustedOut.handshake().remoteRSA()));

        // TODO fix reply attack

        long timestamp = ByteBuffer.wrap(payload).getLong();

        throw new TunnelClientHandshakeExpiredException(fingerprint, timestamp);
    }
}

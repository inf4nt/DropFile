package com.evolution.dropfiledaemon.tunnel.framework.client.handler;

import com.evolution.dropfiledaemon.handshake.store.HandshakeTrustedOutStore;
import com.evolution.dropfiledaemon.tunnel.framework.client.exception.TunnelClientException;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.InputStream;

public interface TunnelClientHandler {

    int getStatusCode();

    InputStream handle(String fingerprint,
                       HandshakeTrustedOutStore.TrustedOut trustedOut,
                       SecretKey secretKey,
                       InputStream inputStream) throws TunnelClientException, IOException;
}

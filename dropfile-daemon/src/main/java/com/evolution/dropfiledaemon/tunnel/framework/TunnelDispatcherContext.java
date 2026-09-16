package com.evolution.dropfiledaemon.tunnel.framework;

import com.evolution.dropfile.common.io.OnceCloseableInputStream;

import javax.crypto.SecretKey;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public record TunnelDispatcherContext(String fingerprint,
                                      SecretKey secretKey,
                                      TunnelRequestDTO.Payload requestPayload,
                                      OnceCloseableInputStream inputStream) implements Closeable {

    public TunnelDispatcherContext(String fingerprint,
                                   SecretKey secretKey,
                                   TunnelRequestDTO.Payload requestPayload,
                                   InputStream inputStream) {
        this(
                Objects.requireNonNull(fingerprint),
                Objects.requireNonNull(secretKey),
                Objects.requireNonNull(requestPayload),
                new OnceCloseableInputStream(inputStream)
        );
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }
}

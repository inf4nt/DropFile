package com.evolution.dropfiledaemon.tunnel.framework;

import com.evolution.dropfile.common.io.OnceCloseableInputStream;

import javax.crypto.SecretKey;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class TunnelDispatcherContext implements Closeable {

    private final String fingerprint;

    private final SecretKey secretKey;

    private final TunnelRequestDTO.Payload requestPayload;

    private final OnceCloseableInputStream inputStream;

    public TunnelDispatcherContext(String fingerprint,
                                   SecretKey secretKey,
                                   TunnelRequestDTO.Payload requestPayload,
                                   InputStream inputStream) {
        this.fingerprint = Objects.requireNonNull(fingerprint);
        this.secretKey = Objects.requireNonNull(secretKey);
        this.requestPayload = Objects.requireNonNull(requestPayload);
        this.inputStream = new OnceCloseableInputStream(inputStream);
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public SecretKey getSecretKey() {
        return secretKey;
    }

    public TunnelRequestDTO.Payload getRequestPayload() {
        return requestPayload;
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }
}

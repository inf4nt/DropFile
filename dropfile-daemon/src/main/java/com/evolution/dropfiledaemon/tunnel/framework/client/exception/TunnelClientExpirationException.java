package com.evolution.dropfiledaemon.tunnel.framework.client.exception;

import lombok.Getter;

public class TunnelClientExpirationException extends TunnelClientException {

    @Getter
    private final long timestamp;

    public TunnelClientExpirationException(String fingerprint, long timestamp) {
        super(fingerprint);
        this.timestamp = timestamp;
    }

    public TunnelClientExpirationException(String fingerprint, String message, Throwable throwable, long timestamp) {
        super(fingerprint, message, throwable);
        this.timestamp = timestamp;
    }

    public TunnelClientExpirationException(String fingerprint, Throwable throwable, long timestamp) {
        super(fingerprint, throwable);
        this.timestamp = timestamp;
    }
}

package com.evolution.dropfiledaemon.tunnel.framework.exception;

import lombok.Getter;

public class TunnelClientException extends Exception {

    @Getter
    private final String fingerprint;

    public TunnelClientException(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public TunnelClientException(String fingerprint, String message, Throwable throwable) {
        this.fingerprint = fingerprint;
        super(message, throwable);
    }

    public TunnelClientException(String fingerprint, Throwable throwable) {
        this.fingerprint = fingerprint;
        super(throwable);
    }
}

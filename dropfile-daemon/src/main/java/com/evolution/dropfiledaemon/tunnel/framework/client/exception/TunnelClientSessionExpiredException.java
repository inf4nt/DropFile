package com.evolution.dropfiledaemon.tunnel.framework.client.exception;

public class TunnelClientSessionExpiredException
        extends TunnelClientExpirationException {

    public TunnelClientSessionExpiredException(String fingerprint, long timestamp) {
        super(fingerprint, timestamp);
    }
}

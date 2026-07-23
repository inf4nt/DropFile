package com.evolution.dropfiledaemon.tunnel.framework.client.exception;

public class TunnelClientHandshakeExpiredException
        extends TunnelClientExpirationException {

    public TunnelClientHandshakeExpiredException(String fingerprint, long timestamp) {
        super(fingerprint, timestamp);
    }
}

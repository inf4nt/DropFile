package com.evolution.dropfiledaemon.tunnel.framework.exception;

// TODO make it
public class TunnelClientHandshakeExpiredException extends TunnelClientExpirationException {

    public TunnelClientHandshakeExpiredException(String fingerprint, long timestamp) {
        super(fingerprint, timestamp);
    }
}

package com.evolution.dropfiledaemon.handshake.exception;

public class HandshakeAlreadyTrustedException extends RuntimeException {
    public HandshakeAlreadyTrustedException(String message) {
        super("Already trusted for fingerprint " + message);
    }
}

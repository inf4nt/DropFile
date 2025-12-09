package com.evolution.dropfiledaemon.handshake.exception;

public class NoIncomingRequestFoundException extends RuntimeException {
    public NoIncomingRequestFoundException(String fingerprint) {
        super("No incoming request found for fingerprint " + fingerprint);
    }
}

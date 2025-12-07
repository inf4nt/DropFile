package com.evolution.dropfiledaemon.handshake.exception;

public class ApiHandshakeNoIncomingRequestFoundException extends RuntimeException {
    public ApiHandshakeNoIncomingRequestFoundException(String fingerprint) {
        super("No incoming request found for fingerprint " + fingerprint);
    }
}

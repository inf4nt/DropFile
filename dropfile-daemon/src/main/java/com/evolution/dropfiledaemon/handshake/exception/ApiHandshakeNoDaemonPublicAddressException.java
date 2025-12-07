package com.evolution.dropfiledaemon.handshake.exception;

public class ApiHandshakeNoDaemonPublicAddressException extends RuntimeException {
    public ApiHandshakeNoDaemonPublicAddressException() {
        super("No public daemon address. Use CLI command to set it");
    }
}

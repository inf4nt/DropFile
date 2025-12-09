package com.evolution.dropfiledaemon.handshake.exception;

public class NoDaemonPublicAddressException extends RuntimeException {
    public NoDaemonPublicAddressException() {
        super("No public daemon address. Use CLI command to set it");
    }
}

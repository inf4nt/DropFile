package com.evolution.dropfiledaemon.exception;

public class ApiFacadePingNodeException extends RuntimeException {
    public ApiFacadePingNodeException(String fingerprint) {
        super("Ping failed. No access to: " + fingerprint);
    }
}

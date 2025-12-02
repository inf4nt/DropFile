package com.evolution.dropfiledaemon.exception;

public class ConnectionFacadeException extends RuntimeException {
    public ConnectionFacadeException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConnectionFacadeException(String message) {
        super(message);
    }
}

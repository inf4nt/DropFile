package com.evolution.dropfiledaemon.handshake;

import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.net.ConnectException;

@ControllerAdvice(assignableTypes = ApiHandshakeRestController.class)
public class ApiHandshakeRestControllerExceptionHandler {

    @ExceptionHandler(ConnectException.class)
    public ResponseEntity<String> connectException(ConnectException e) {
        if (ObjectUtils.isEmpty(e.getMessage())) {
            return ResponseEntity.badRequest().body(
                    e.getClass().getName()
            );
        }
        return ResponseEntity.badRequest().body(
                e.getClass().getName() + ". Message: " + e.getMessage()
        );
    }
}

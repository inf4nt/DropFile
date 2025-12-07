package com.evolution.dropfiledaemon.handshake;

import com.evolution.dropfiledaemon.handshake.exception.HandshakeAlreadyTrustedException;
import com.evolution.dropfiledaemon.handshake.exception.ApiHandshakeNoIncomingRequestFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice(assignableTypes = ApiHandshakeRestController.class)
public class ApiHandshakeRestControllerExceptionHandler {

    @ExceptionHandler({HandshakeAlreadyTrustedException.class})
    public ResponseEntity<?> alreadyTrusted(Exception e) {
        return ResponseEntity.status(409).body(e.getMessage());
    }

    @ExceptionHandler(ApiHandshakeNoIncomingRequestFoundException.class)
    public ResponseEntity<?> noIncomingRequest(Exception e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}

package com.evolution.dropfiledaemon.handshake;

import com.evolution.dropfiledaemon.handshake.exception.HandshakeRequestAlreadyTrustedException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.NoSuchElementException;

@ControllerAdvice(assignableTypes = HandshakeRestController.class)
public class HandshakeRestControllerExceptionHandler {

    @ExceptionHandler({HandshakeRequestAlreadyTrustedException.class})
    public ResponseEntity<?> alreadyTrusted(Exception e) {
        return ResponseEntity.status(409).body(e);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<?> noSuchElement() {
        return ResponseEntity.badRequest().body("No such element");
    }
}

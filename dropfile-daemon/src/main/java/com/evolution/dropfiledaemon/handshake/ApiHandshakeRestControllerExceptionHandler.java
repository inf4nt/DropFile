package com.evolution.dropfiledaemon.handshake;

import com.evolution.dropfiledaemon.handshake.exception.NoDaemonPublicAddressException;
import com.evolution.dropfiledaemon.handshake.exception.NoIncomingRequestFoundException;
import com.evolution.dropfiledaemon.handshake.exception.HandshakeAlreadyTrustedException;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.net.ConnectException;

@ControllerAdvice(assignableTypes = ApiHandshakeRestController.class)
public class ApiHandshakeRestControllerExceptionHandler {

    @ExceptionHandler({HandshakeAlreadyTrustedException.class})
    public ResponseEntity<String> alreadyTrusted(Exception e) {
        return ResponseEntity.status(409).body(e.getMessage());
    }

    @ExceptionHandler(NoIncomingRequestFoundException.class)
    public ResponseEntity<String> noIncomingRequest(Exception e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(NoDaemonPublicAddressException.class)
    public ResponseEntity<String> noDaemonPublicAddress(Exception e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(ConnectException.class)
    public ResponseEntity<String> connectException(Exception e) {
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

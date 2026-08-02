package com.evolution.dropfiledaemon.tunnel;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice(assignableTypes = TunnelServerRestController.class)
public class TunnelRestControllerExceptionHandler {

    @ExceptionHandler({Exception.class})
    public ResponseEntity<?> handleAsyncTimeoutException(Exception exception) {
        log.error("Tunnel exception: {}", exception.getMessage(), exception);
        return ResponseEntity.notFound().build();
    }
}

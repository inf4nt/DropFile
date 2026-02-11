package com.evolution.dropfiledaemon.tunnel;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice(assignableTypes = TunnelRestController.class)
public class TunnelRestControllerExceptionHandler {

    @ExceptionHandler({Exception.class})
    public ResponseEntity<?> handleAsyncTimeoutException(Exception exception) {
        log.info("Tunnel exception: {}", exception.getMessage(), exception);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .build();
    }
}

package com.evolution.dropfiledaemon.handshake;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice(assignableTypes = ApiHandshakeRestController.class)
public class ApiHandshakeRestControllerExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> exception(Exception e) {
        log.info(e.getMessage(), e);
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

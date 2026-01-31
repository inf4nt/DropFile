package com.evolution.dropfiledaemon.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice(assignableTypes = {
        ApiConnectionsAccessRestController.class,
        ApiConnectionsShareRestController.class,
        ApiConnectionsRestController.class,
        ApiDaemonRestController.class,
        ApiShareRestController.class,
        ApiDownloadRestController.class
})
public class ApiControllerExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> exception(Exception e) {
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

package com.evolution.dropfiledaemon.controller;

import com.evolution.dropfiledaemon.exception.ApiFacadePingNodeException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice(assignableTypes = ApiRestController.class)
public class ApiRestControllerExceptionHandler {

    @ExceptionHandler({ApiFacadePingNodeException.class})
    public ResponseEntity<String> pingNode(Exception e) {
        return ResponseEntity.status(401).body(e.getMessage());
    }
}

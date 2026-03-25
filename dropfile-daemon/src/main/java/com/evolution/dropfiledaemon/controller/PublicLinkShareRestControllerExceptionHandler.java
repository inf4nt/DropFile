package com.evolution.dropfiledaemon.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice(assignableTypes = PublicLinkShareRestController.class)
public class PublicLinkShareRestControllerExceptionHandler {

    @ExceptionHandler({Exception.class})
    public ResponseEntity<?> exceptionHandler(Exception exception) {
        log.info("PublicLinkShareRestController exception: {}", exception.getMessage(), exception);
        return ResponseEntity.notFound().build();
    }
}

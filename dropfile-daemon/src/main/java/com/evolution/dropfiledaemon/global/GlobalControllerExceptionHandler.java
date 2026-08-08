package com.evolution.dropfiledaemon.global;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class GlobalControllerExceptionHandler {

    @ExceptionHandler({Exception.class})
    public ResponseEntity<?> exception(Exception exception, HttpServletRequest request) {
        log.error("Web controller error: {}", exception.getMessage(), exception);
        if (isApiCall(request)) {
            if (ObjectUtils.isEmpty(exception.getMessage())) {
                return ResponseEntity.badRequest().body(
                        exception.getClass().getName()
                );
            }
            return ResponseEntity.badRequest().body(
                    exception.getClass().getName() + ". Message: " + exception.getMessage()
            );
        }
        return ResponseEntity.notFound().build();
    }

    private boolean isApiCall(HttpServletRequest request) {
        return request != null && request.getServletPath() != null && request.getServletPath().startsWith("/api");
    }
}

package com.evolution.dropfiledaemon.controller;

import com.evolution.dropfiledaemon.security.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
@ControllerAdvice
public class GlobalControllerExceptionHandler {

    private final TokenService tokenService;

    /**
     * Global exception handler enforcing a strict security posture.
     *
     * Design decisions:
     * 1. Uniform 404 Fallback: For any unauthenticated or unauthorized requests, we return
     *    HTTP 404 instead of 403/401 or leaking error details. This mimics GitHub's approach
     *    to private or non-existent resources—preventing attackers from probing the system
     *    or mapping out valid API endpoints/IDs through error responses (information disclosure).
     *
     * 2. Trusted Local CLI Channel: Valid CLI clients authenticated via a loopback-bound token
     *    (127.0.0.1 -> 127.0.0.1) receive safe, descriptive error messages with a 400 status
     *    for debugging. Because the auth token never traverses an external network, it cannot
     *    be intercepted remotely. If an attacker has local access to siphon the token, the host
     *    machine is already fully compromised.
     */
    @ExceptionHandler({Exception.class})
    public ResponseEntity<?> exception(Exception exception, HttpServletRequest request) {
        log.error("Web controller error: {}", exception.getMessage(), exception);
        if (isCliApiCall(request)) {
            if (StringUtils.hasText(exception.getMessage())) {
                return ResponseEntity.badRequest().body(exception.getMessage());
            }
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.notFound().build();
    }

    private boolean isCliApiCall(HttpServletRequest request) {
        if (request == null || request.getServletPath() == null) {
            return false;
        }

        if (request.getServletPath().startsWith("/api")) {
            UUID token = tokenService.extractToken(request);
            return tokenService.isValid(token);
        }

        return false;
    }
}

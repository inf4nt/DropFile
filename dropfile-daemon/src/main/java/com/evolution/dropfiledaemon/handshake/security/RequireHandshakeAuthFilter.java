package com.evolution.dropfiledaemon.handshake.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RequireHandshakeAuthFilter extends OncePerRequestFilter {

    private static final String PATH_PATTERN = "/node/**";

    private final HandshakeSecretTokenService handshakeSecretTokenService;

    @Autowired
    public RequireHandshakeAuthFilter(HandshakeSecretTokenService handshakeSecretTokenService) {
        this.handshakeSecretTokenService = handshakeSecretTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (isRequireHandshake(request)) {
            String encryptedToken = extractToken(request);
            if (handshakeSecretTokenService.isValid(encryptedToken)) {
                filterChain.doFilter(request, response);
                return;
            }
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }

        filterChain.doFilter(request, response);
    }

    private boolean isRequireHandshake(HttpServletRequest request) {
        return request.getServletPath().startsWith("/node");
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("X-Encrypted-Token");
        if (header == null || header.isEmpty()) {
            return null;
        }
        return header;
    }

    public String getPathPattern() {
        return PATH_PATTERN;
    }
}

package com.evolution.dropfiledaemon.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApplicationAuthFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    private final HandshakeSecretTokenService handshakeSecretTokenService;

    @Autowired
    public ApplicationAuthFilter(TokenService tokenService,
                                 HandshakeSecretTokenService handshakeSecretTokenService) {
        this.tokenService = tokenService;
        this.handshakeSecretTokenService = handshakeSecretTokenService;
    }

    private boolean isApi(HttpServletRequest request) {
        return request.getServletPath().startsWith("/api");
    }

    private boolean isHandshake(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        return servletPath.startsWith("/handshake");
    }

    private boolean isNode(HttpServletRequest request) {
        return request.getServletPath().startsWith("/node");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (isHandshake(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isApi(request)) {
            String token = extractToken(request);
            if (tokenService.isValid(token)) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        if (isNode(request)) {
            String tokenBase64 = extractToken(request);
            if (handshakeSecretTokenService.isValid(tokenBase64)) {
                filterChain.doFilter(request, response);
                return;
            }
        }


        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        return header.substring(7);
    }
}

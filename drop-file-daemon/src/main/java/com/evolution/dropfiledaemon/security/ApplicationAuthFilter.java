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

    @Autowired
    public ApplicationAuthFilter(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    private boolean isHandshake(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        return servletPath.startsWith("/handshake");
    }

    // TODO drop it
    private boolean isApiHandshake(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        return servletPath.startsWith("/api/handshake/request");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (isHandshake(request) || isApiHandshake(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = extractToken(request);
        if (!tokenService.isValid(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        return header.substring(7);
    }
}

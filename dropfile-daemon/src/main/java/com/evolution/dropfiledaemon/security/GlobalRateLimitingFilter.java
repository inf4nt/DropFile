package com.evolution.dropfiledaemon.security;

import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import com.evolution.dropfiledaemon.controller.server.ServerHandshakeRestController;
import com.evolution.dropfiledaemon.controller.server.ServerQuickShareRestController;
import com.evolution.dropfiledaemon.controller.server.ServerTunnelRestController;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Component
public class GlobalRateLimitingFilter extends OncePerRequestFilter {

    private static final Set<String> HANDSHAKE_ENDPOINTS = Set.of(
            ServerHandshakeRestController.HANDSHAKE_ENDPOINT,
            ServerHandshakeRestController.HANDSHAKE_SESSION_ENDPOINT
    );

    private final Semaphore handshakeSemaphore;

    private final Semaphore tunnelSemaphore;

    private final Semaphore quickshareSemaphore;

    public GlobalRateLimitingFilter(DaemonApplicationProperties properties) {
        this.handshakeSemaphore = new Semaphore(
                properties.daemonServerServletRateRequestHandshakeLimitMax,
                true
        );
        this.tunnelSemaphore = new Semaphore(
                properties.daemonServerServletRateRequestTunnelLimitMax,
                true
        );
        this.quickshareSemaphore = new Semaphore(
                properties.daemonServerServletRateRequestQuickshareLimitMax,
                true
        );
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String servletPath = request.getServletPath();
        if (!StringUtils.hasText(servletPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (HANDSHAKE_ENDPOINTS.contains(servletPath)) {
            executeWithSemaphore(handshakeSemaphore, request, response, filterChain);
        } else if (ServerTunnelRestController.TUNNEL_ENDPOINT.equals(servletPath)) {
            executeWithSemaphore(tunnelSemaphore, request, response, filterChain);
        } else if (servletPath.startsWith(ServerQuickShareRestController.QUICKSHARE_ENDPOINT)) {
            executeWithSemaphore(quickshareSemaphore, request, response, filterChain);
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private void executeWithSemaphore(Semaphore semaphore,
                                      HttpServletRequest request,
                                      HttpServletResponse response,
                                      FilterChain filterChain) throws ServletException, IOException {
        boolean acquired;
        try {
            acquired = semaphore.tryAcquire(200, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            return;
        }

        if (!acquired) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            return;
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            semaphore.release();
        }
    }
}

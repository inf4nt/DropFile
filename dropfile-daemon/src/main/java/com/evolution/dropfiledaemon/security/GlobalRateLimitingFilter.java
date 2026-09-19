package com.evolution.dropfiledaemon.security;

import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import com.evolution.dropfiledaemon.controller.server.ServerHandshakeRestController;
import com.evolution.dropfiledaemon.controller.server.ServerQuickShareRestController;
import com.evolution.dropfiledaemon.controller.server.ServerTunnelRestController;
import jakarta.annotation.Nullable;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
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
        Semaphore semaphore = resolveSemaphore(servletPath);

        if (semaphore == null) {
            filterChain.doFilter(request, response);
            return;
        }

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

        boolean asyncListenerAdded = false;
        AtomicBoolean released = new AtomicBoolean(false);

        Runnable releaseOnce = () -> {
            if (released.compareAndSet(false, true)) {
                semaphore.release();
            }
        };

        try {
            filterChain.doFilter(request, response);

            if (request.isAsyncStarted()) {
                try {
                    request.getAsyncContext().addListener(new AsyncListener() {
                        @Override
                        public void onComplete(AsyncEvent event) {
                            releaseOnce.run();
                        }

                        @Override
                        public void onTimeout(AsyncEvent event) {
                            releaseOnce.run();
                        }

                        @Override
                        public void onError(AsyncEvent event) {
                            releaseOnce.run();
                        }

                        @Override
                        public void onStartAsync(AsyncEvent event) {
                        }
                    });
                    asyncListenerAdded = true;
                } catch (Exception e) {
                    log.error("Failed to add async listener for rate limiting: {}", e.getMessage(), e);
                }
            }
        } finally {
            if (!asyncListenerAdded) {
                releaseOnce.run();
            }
        }
    }

    @Nullable
    private Semaphore resolveSemaphore(String path) {
        if (path == null) {
            return null;
        }
        if (HANDSHAKE_ENDPOINTS.contains(path)) {
            return handshakeSemaphore;
        }
        if (path.startsWith(ServerTunnelRestController.TUNNEL_ENDPOINT)) {
            return tunnelSemaphore;
        }
        if (path.startsWith(ServerQuickShareRestController.QUICKSHARE_ENDPOINT)) {
            return quickshareSemaphore;
        }
        return null;
    }
}

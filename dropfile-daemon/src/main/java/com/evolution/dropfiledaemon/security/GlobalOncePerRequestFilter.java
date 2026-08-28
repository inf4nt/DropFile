package com.evolution.dropfiledaemon.security;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfiledaemon.activity.ActivityTracker;
import com.evolution.dropfiledaemon.activity.TrafficAwareResponseWrapper;
import com.evolution.dropfiledaemon.controller.server.ServerHandshakeRestController;
import com.evolution.dropfiledaemon.controller.server.ServerQuickShareRestController;
import com.evolution.dropfiledaemon.controller.server.ServerTunnelRestController;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@RequiredArgsConstructor
@Component
public class GlobalOncePerRequestFilter extends OncePerRequestFilter {

    private static final String API_PREFIX = "/api";

    private final TokenService tokenService;

    private final ActivityTracker activityTracker;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getServletPath();

        if (path != null && path.startsWith(API_PREFIX)) {
            UUID token = tokenService.extractToken(request);
            if (tokenService.isValid(token)) {
                activityTracker.markApiRequest(request);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        }

        if (isTrackedEndpoint(path)) {
            activityTracker.requestStarted();
            boolean asyncListenerAdded = false;
            AtomicBoolean handled = new AtomicBoolean(false);

            Runnable runOnceCompletion = () -> {
                if (handled.compareAndSet(false, true)) {
                    handleCompletion(request, response);
                }
            };

            try {
                HttpServletResponseWrapper wrappedResponse = new TrafficAwareResponseWrapper(request, response, activityTracker);

                filterChain.doFilter(request, wrappedResponse);

                if (request.isAsyncStarted()) {
                    try {
                        request.getAsyncContext().addListener(new AsyncListener() {
                            @Override
                            public void onComplete(AsyncEvent event) {
                                runOnceCompletion.run();
                            }

                            @Override
                            public void onTimeout(AsyncEvent event) {
                                runOnceCompletion.run();
                            }

                            @Override
                            public void onError(AsyncEvent event) {
                                runOnceCompletion.run();
                            }

                            @Override
                            public void onStartAsync(AsyncEvent event) {
                            }
                        });
                        asyncListenerAdded = true;
                    } catch (Exception e) {
                        log.error("Add async listener error: {}", e.getMessage(), e);
                    }
                }
            } finally {
                if (!asyncListenerAdded) {
                    runOnceCompletion.run();
                }
            }
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isTrackedEndpoint(String path) {
        if (path == null) {
            return false;
        }
        return path.startsWith(API_PREFIX) ||
                path.startsWith(CommonUtils.joinPaths("/" + ServerTunnelRestController.TUNNEL_ENDPOINT)) ||
                path.startsWith(CommonUtils.joinPaths("/" + ServerHandshakeRestController.HANDSHAKE_ENDPOINT)) ||
                path.startsWith(CommonUtils.joinPaths("/" + ServerHandshakeRestController.HANDSHAKE_SESSION_ENDPOINT)) ||
                path.startsWith(CommonUtils.joinPaths("/" + ServerQuickShareRestController.QUICKSHARE_ENDPOINT));
    }

    private void handleCompletion(HttpServletRequest request, HttpServletResponse response) {
        if (activityTracker.shouldRecordActivity(request, response)) {
            activityTracker.recordActivity();
        }
        activityTracker.requestEnded();
    }
}
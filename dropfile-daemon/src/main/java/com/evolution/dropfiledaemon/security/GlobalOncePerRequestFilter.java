package com.evolution.dropfiledaemon.security;

import com.evolution.dropfiledaemon.activity.ActivityTracker;
import com.evolution.dropfiledaemon.activity.TrafficAwareResponseWrapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

@RequiredArgsConstructor
@Component
public class GlobalOncePerRequestFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    private final ActivityTracker activityTracker;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getServletPath();

        if (path != null && path.startsWith("/api")) {
            String token = extractToken(request);
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
                    AsyncContext asyncContext = request.getAsyncContext();
                    asyncContext.addListener(new AsyncListener() {
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
                            System.out.println();
                        }
                    });
                    asyncListenerAdded = true;
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
        return path.startsWith("/api") ||
                path.startsWith("/public/tunnel") ||
                path.startsWith("/public/handshake") ||
                path.startsWith("/p/qs");
    }

    private void handleCompletion(HttpServletRequest request, HttpServletResponse response) {
        if (activityTracker.shouldRecordActivity(request, response)) {
            activityTracker.recordActivity();
        }
        activityTracker.requestEnded();
    }

    private String extractToken(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        return header.substring(7);
    }
}

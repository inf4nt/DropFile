package com.evolution.dropfiledaemon.activity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class ActivityTracker {

    public static final String API_REQUEST_ATTRIBUTE = "API_REQUEST_ATTRIBUTE";

    private final AtomicLong lastActivityTime = new AtomicLong(System.currentTimeMillis());

    private final AtomicInteger activeRequests = new AtomicInteger(0);

    public void recordActivity() {
        lastActivityTime.set(System.currentTimeMillis());
    }

    public void requestStarted() {
        activeRequests.incrementAndGet();
    }

    public void requestEnded() {
        activeRequests.decrementAndGet();
    }

    /**
     * Evaluates whether the application is considered idle and should trigger a daemon shutdown.
     *
     * Timeout Definitions:
     *
     * 1. Soft Idle Timeout:
     *    Graceful shutdown condition. Triggers ONLY when there are zero active HTTP requests
     *    (activeRequests == 0) AND the time since the last recorded activity exceeds the
     *    configured soft threshold. This guarantees active transfers (e.g., file downloads)
     *    are never interrupted.
     *
     * 2. Hard Idle Timeout:
     *    Safety-net fallback condition. Forces daemon shutdown once inactivity exceeds
     *    the hard threshold regardless of active requests (activeRequests > 0).
     *    This prevents zombie processes caused by leaked connections, unclosed IO streams,
     *    or abnormal client disconnects.
     *
     * @param idleTimeoutMillis  The soft idle threshold in milliseconds.
     * @param hardTimeoutMillis  The hard idle threshold in milliseconds (absolute safety limit).
     * @return true if the system meets either soft or hard idle criteria; false otherwise.
     */
    public boolean isIdle(long idleTimeoutMillis, long hardTimeoutMillis) {
        long now = System.currentTimeMillis();
        long timeSinceLastActivity = now - lastActivityTime.get();

        if (timeSinceLastActivity > hardTimeoutMillis) {
            return true;
        }

        return activeRequests.get() == 0 && timeSinceLastActivity > idleTimeoutMillis;
    }

    public boolean shouldRecordActivity(HttpServletRequest request,
                                        HttpServletResponse response) {
        if (request == null || response == null) {
            return false;
        }

        // To track any valid(token is ok) API request
        if (Boolean.TRUE.equals(request.getAttribute(API_REQUEST_ATTRIBUTE))) {
            return true;
        }

        int status = response.getStatus();
        return status >= 200 && status < 400;
    }

    public void markApiRequest(HttpServletRequest request) {
        request.setAttribute(API_REQUEST_ATTRIBUTE, true);
    }
}

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

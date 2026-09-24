package com.evolution.dropfiledaemon.activity;

import com.evolution.dropfiledaemon.DropFileDaemonApplication;
import com.evolution.dropfiledaemon.bootstrap.DropFileDaemonApplicationReadyEvent;
import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
public class IdleShutdownMonitor {

    private final ActivityTracker activityTracker;

    private final TaskScheduler taskScheduler;

    private final Duration daemonIdleRateInterval;

    private final Duration softDaemonIdleShutdownTimeout;

    private final Duration hardDaemonIdleShutdownTimeout;

    public IdleShutdownMonitor(ActivityTracker activityTracker,
                               TaskScheduler taskScheduler,
                               DaemonApplicationProperties applicationProperties) {
        this.activityTracker = activityTracker;
        this.taskScheduler = taskScheduler;
        this.daemonIdleRateInterval = applicationProperties.daemonIdleRateInterval;
        this.softDaemonIdleShutdownTimeout = applicationProperties.daemonIdleShutdownTimeout;
        this.hardDaemonIdleShutdownTimeout = this.softDaemonIdleShutdownTimeout.multipliedBy(2);
    }

    @EventListener(DropFileDaemonApplicationReadyEvent.class)
    public void listener() {
        log.info("Application is ready. Initializing idle shutdown monitor (rate: {}ms)", daemonIdleRateInterval.toMillis());
        log.info("Application is ready. Initializing idle shutdown monitor (soft timeout: {}ms)", softDaemonIdleShutdownTimeout.toMillis());
        log.info("Application is ready. Initializing idle shutdown monitor (hard timeout: {}ms)", hardDaemonIdleShutdownTimeout.toMillis());

        if (!daemonIdleRateInterval.isPositive()) {
            log.info("Idle shutdown monitor is not running. Rate is negative or zero");
            return;
        }

        if (!softDaemonIdleShutdownTimeout.isPositive()) {
            log.info("Idle shutdown monitor is not running. Idle timeout millis is negative or zero");
            return;
        }

        taskScheduler.scheduleWithFixedDelay(
                this::scheduler,
                daemonIdleRateInterval
        );
    }

    private void scheduler() {
        long softDaemonIdleShutdownTimeoutMillis = softDaemonIdleShutdownTimeout.toMillis();
        long hardDaemonIdleShutdownTimeoutMillis = hardDaemonIdleShutdownTimeout.toMillis();
        if (activityTracker.isIdle(softDaemonIdleShutdownTimeoutMillis, hardDaemonIdleShutdownTimeoutMillis)) {
            log.info("No activity detected for soft {} ms hard {} ms. Shutting down server to save resources...",
                    softDaemonIdleShutdownTimeoutMillis, hardDaemonIdleShutdownTimeoutMillis
            );
            DropFileDaemonApplication.exit();
        }
    }
}

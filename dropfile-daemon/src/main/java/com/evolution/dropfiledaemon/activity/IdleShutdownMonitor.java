package com.evolution.dropfiledaemon.activity;

import com.evolution.dropfiledaemon.DropFileDaemonApplication;
import com.evolution.dropfiledaemon.bootstrap.event.DropFileDaemonApplicationReadyEvent;
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

    private final long daemonIdleRateMillis;

    private final long softDaemonIdleTimeoutMillis;

    private final long hardDaemonIdleTimeoutMillis;

    public IdleShutdownMonitor(ActivityTracker activityTracker,
                               TaskScheduler taskScheduler,
                               DaemonApplicationProperties applicationProperties) {
        this.activityTracker = activityTracker;
        this.taskScheduler = taskScheduler;
        this.daemonIdleRateMillis = applicationProperties.daemonIdleRateMillis;
        this.softDaemonIdleTimeoutMillis = applicationProperties.daemonIdleTimeoutMillis;
        this.hardDaemonIdleTimeoutMillis = 2 * applicationProperties.daemonIdleTimeoutMillis;
    }

    @EventListener(DropFileDaemonApplicationReadyEvent.class)
    public void listener() {
        log.info("Application is ready. Initializing idle shutdown monitor (rate: {}ms)", daemonIdleRateMillis);
        log.info("Application is ready. Initializing idle shutdown monitor (soft timeout: {}ms)", softDaemonIdleTimeoutMillis);
        log.info("Application is ready. Initializing idle shutdown monitor (hard timeout: {}ms)", hardDaemonIdleTimeoutMillis);

        if (daemonIdleRateMillis <= 0) {
            log.info("Idle shutdown monitor is not running. Rate is negative");
            return;
        }

        if (softDaemonIdleTimeoutMillis <= 0) {
            log.info("Idle shutdown monitor is not running. Idle timeout millis is negative");
            return;
        }

        taskScheduler.scheduleWithFixedDelay(
                this::scheduler,
                Duration.ofMillis(daemonIdleRateMillis)
        );
    }

    private void scheduler() {
        if (activityTracker.isIdle(softDaemonIdleTimeoutMillis, hardDaemonIdleTimeoutMillis)) {
            log.info("No activity detected for soft {} ms hard {} ms. Shutting down server to save resources...",
                    softDaemonIdleTimeoutMillis, hardDaemonIdleTimeoutMillis
            );
            DropFileDaemonApplication.exit();
        }
    }
}

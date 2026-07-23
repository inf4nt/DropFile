package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.Purgeable;
import com.evolution.dropfiledaemon.bootstrap.event.DropFileDaemonApplicationReadyEvent;
import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@RequiredArgsConstructor
@Component
public class GarbageCollectorFacade {

    private final AtomicBoolean isPurgeRunning = new AtomicBoolean(false);

    private final List<Purgeable> purgeables;

    private final DaemonApplicationProperties applicationProperties;

    volatile private long lastRunTimestamp = 0;

    private final TaskScheduler taskScheduler;

    @EventListener(DropFileDaemonApplicationReadyEvent.class)
    public void readyListener() {
        long rateMillis = applicationProperties.daemonGcRateMillis;

        log.info("Application is ready. Initializing GC task scheduler (rate: {}ms)", rateMillis);

        Instant firstRunTime = Instant.now().plusMillis(rateMillis);

        taskScheduler.scheduleWithFixedDelay(
                this::purgeScheduler,
                firstRunTime,
                Duration.ofMillis(rateMillis)
        );
    }

    public void purge() {
        if (!isPurgeRunning.compareAndSet(false, true)) {
            log.debug("Garbage collection is already in progress. Skipping request");
            return;
        }

        try {
            log.debug("Starting garbage collection");

            for (Purgeable purgeable : purgeables) {
                String purgeableName = AopUtils.getTargetClass(purgeable).getSimpleName();
                try {
                    log.debug("Purging {}", purgeableName);
                    purgeable.purge();
                } catch (Exception e) {
                    log.error("Failed to purge component {}", purgeableName, e);
                }
            }

            lastRunTimestamp = System.currentTimeMillis();
            log.debug("Garbage collection completed successfully");
        } finally {
            isPurgeRunning.set(false);
        }
    }

    private void purgeScheduler() {
        long rate = applicationProperties.daemonGcRateMillis;
        long elapsedSinceLastRun = System.currentTimeMillis() - lastRunTimestamp;

        if (lastRunTimestamp != 0 && elapsedSinceLastRun < rate) {
            log.debug("Skipping scheduled GC. UI or manual purge was executed {}ms ago (rate is {}ms)",
                    elapsedSinceLastRun, rate);
            return;
        }

        purge();
    }
}

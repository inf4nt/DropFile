package com.evolution.dropfiledaemon.bootstrap;

import com.evolution.dropfiledaemon.bootstrap.event.DropFileDaemonApplicationReadyEvent;
import com.evolution.dropfiledaemon.bootstrap.event.DropFileDaemonBeforeApplicationReadyEvent;
import com.evolution.dropfiledaemon.bootstrap.phase.ApplicationInitializationPhase;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class ApplicationPhaseBootstrapRunner {

    private final List<ApplicationInitializationPhase> phases;

    private final ApplicationEventPublisher eventPublisher;

    @EventListener(ApplicationReadyEvent.class)
    public void listener() throws Exception {
        for (ApplicationInitializationPhase phase : phases) {
            phase.execute();
        }

        eventPublisher.publishEvent(new DropFileDaemonBeforeApplicationReadyEvent());
        eventPublisher.publishEvent(new DropFileDaemonApplicationReadyEvent());
    }
}

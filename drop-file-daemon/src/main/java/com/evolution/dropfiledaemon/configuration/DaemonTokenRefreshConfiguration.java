package com.evolution.dropfiledaemon.configuration;

import com.evolution.dropfile.configuration.secret.DropFileSecretsConfigFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DaemonTokenRefreshConfiguration implements ApplicationListener<ApplicationReadyEvent> {

    private final DropFileSecretsConfigFacade secretsConfigFacade;

    @Autowired
    public DaemonTokenRefreshConfiguration(DropFileSecretsConfigFacade secretsConfigFacade) {
        this.secretsConfigFacade = secretsConfigFacade;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        secretsConfigFacade.refreshDaemonToken();
    }
}

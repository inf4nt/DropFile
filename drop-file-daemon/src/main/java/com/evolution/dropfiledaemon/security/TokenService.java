package com.evolution.dropfiledaemon.security;

import com.evolution.dropfile.configuration.Preconditions;
import com.evolution.dropfile.configuration.secret.DropFileSecretsConfigManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

@Service
public class TokenService implements ApplicationListener<ApplicationReadyEvent> {

    private final DropFileSecretsConfigManager secretsConfig;

    private String daemonToken;

    @Autowired
    public TokenService(DropFileSecretsConfigManager secretsConfig) {
        this.secretsConfig = secretsConfig;
    }

    public boolean isValid(String tokenIncoming) {
        Preconditions.checkState(
                () -> !tokenIncoming.isEmpty(),
                "Token is empty"
        );
        Preconditions.checkState(
                () -> !daemonToken.isEmpty(),
                "Daemon token has not initialized yet"
        );

        return daemonToken.equals(tokenIncoming);
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        secretsConfig.refreshDaemonToken();
        String daemonToken = secretsConfig.get().getDaemonToken();
        Preconditions.checkState(
                () -> !daemonToken.isEmpty(),
                "Configuration Daemon token is empty"
        );
        this.daemonToken = daemonToken;
    }
}


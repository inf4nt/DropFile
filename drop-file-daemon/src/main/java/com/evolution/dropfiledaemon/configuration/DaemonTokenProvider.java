package com.evolution.dropfiledaemon.configuration;

import com.evolution.dropfile.configuration.Preconditions;
import com.evolution.dropfile.configuration.secret.DropFileSecretsConfigManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ObjectUtils;

import java.io.File;
import java.util.Objects;

@Configuration
public class DaemonTokenProvider implements ApplicationListener<ApplicationReadyEvent> {

    @Value("${config.secrets:#{null}}")
    private String customSecretsConfig;

    private String token;

    private final DropFileSecretsConfigManager secretsConfig;

    @Autowired
    public DaemonTokenProvider(DropFileSecretsConfigManager secretsConfig) {
        this.secretsConfig = secretsConfig;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (ObjectUtils.isEmpty(customSecretsConfig)) {
            secretsConfig.refreshDaemonToken();
            this.token = Objects.requireNonNull(secretsConfig.get().getDaemonToken());
        } else {
            String daemonToken = secretsConfig
                    .read(new File(customSecretsConfig))
                    .getDaemonToken();
            this.token = Objects.requireNonNull(daemonToken);
        }
    }

    public String getToken() {
        Preconditions.checkState(() -> !token.isEmpty(), "token has not been set yet");
        return token;
    }
}

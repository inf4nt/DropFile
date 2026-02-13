package com.evolution.dropfiledaemon.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Configuration
public class DropFileWebServerFactoryCustomizer
        implements WebServerFactoryCustomizer<ConfigurableWebServerFactory> {

    private static final Integer DEFAULT_PORT = 18181;

    private final AppConfigStoreUninitialized appConfigStoreUninitialized;

    @Override
    public void customize(ConfigurableWebServerFactory factory) {
        Integer daemonPort = appConfigStoreUninitialized.getUninitializedAppConfigStore()
                .get()
                .map(it -> it.daemonAppConfig())
                .map(it -> it.daemonPort())
                .orElse(DEFAULT_PORT);
        factory.setPort(daemonPort);
    }
}

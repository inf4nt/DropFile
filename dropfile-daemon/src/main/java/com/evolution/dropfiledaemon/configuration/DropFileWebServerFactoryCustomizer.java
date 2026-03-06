package com.evolution.dropfiledaemon.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Configuration
public class DropFileWebServerFactoryCustomizer
        implements WebServerFactoryCustomizer<ConfigurableWebServerFactory> {

    private final AppConfigStoreUninitialized appConfigStoreUninitialized;

    @Override
    public void customize(ConfigurableWebServerFactory factory) {
        Integer daemonPort = appConfigStoreUninitialized.getUninitializedDaemonAppConfigStore()
                .get()
                .map(it -> it.daemonPort())
                .orElse(18181);
        factory.setPort(daemonPort);
    }
}

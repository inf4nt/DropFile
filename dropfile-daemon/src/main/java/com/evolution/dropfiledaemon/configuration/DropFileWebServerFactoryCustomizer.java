package com.evolution.dropfiledaemon.configuration;

import com.evolution.dropfile.store.app.AppConfigStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DropFileWebServerFactoryCustomizer
        implements WebServerFactoryCustomizer<ConfigurableWebServerFactory> {

    private final AppConfigStore appConfigStore;

    @Autowired
    public DropFileWebServerFactoryCustomizer(AppConfigStore appConfigStore) {
        this.appConfigStore = appConfigStore;
    }

    @Override
    public void customize(ConfigurableWebServerFactory factory) {
        Integer daemonPort = appConfigStore.getRequired().daemonAppConfig().daemonPort();
        factory.setPort(daemonPort);
    }
}

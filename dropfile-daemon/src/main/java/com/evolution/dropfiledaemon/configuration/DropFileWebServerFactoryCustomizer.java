package com.evolution.dropfiledaemon.configuration;

import com.evolution.dropfile.configuration.app.DropFileAppConfig;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DropFileWebServerFactoryCustomizer
        implements WebServerFactoryCustomizer<ConfigurableWebServerFactory> {

    private final ObjectProvider<DropFileAppConfig.DropFileDaemonAppConfig> appConfig;

    @Autowired
    public DropFileWebServerFactoryCustomizer(ObjectProvider<DropFileAppConfig.DropFileDaemonAppConfig> appConfig) {
        this.appConfig = appConfig;
    }

    @Override
    public void customize(ConfigurableWebServerFactory factory) {
        Integer daemonPort = appConfig.getObject().daemonPort();
        factory.setPort(daemonPort);
    }
}

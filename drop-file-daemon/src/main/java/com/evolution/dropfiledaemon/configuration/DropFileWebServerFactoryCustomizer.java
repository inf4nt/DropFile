package com.evolution.dropfiledaemon.configuration;

import com.evolution.dropfile.configuration.CommonUtils;
import com.evolution.dropfile.configuration.app.DropFileAppConfig;
import com.evolution.dropfile.configuration.app.DropFileAppConfigManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DropFileWebServerFactoryCustomizer
        implements WebServerFactoryCustomizer<ConfigurableWebServerFactory> {

    private final DropFileAppConfigManager appConfig;

    @Autowired
    public DropFileWebServerFactoryCustomizer(DropFileAppConfigManager appConfig) {
        this.appConfig = appConfig;
    }

    @Override
    public void customize(ConfigurableWebServerFactory factory) {
        DropFileAppConfig config = appConfig.get();
        int port = CommonUtils.toURI(config.getDaemonAddress()).getPort();
        factory.setPort(port);
    }
}

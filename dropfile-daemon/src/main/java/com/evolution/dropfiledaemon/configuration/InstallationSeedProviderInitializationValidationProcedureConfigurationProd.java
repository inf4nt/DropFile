package com.evolution.dropfiledaemon.configuration;

import com.evolution.dropfile.store.framework.file.InstallationSeedProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.ObjectUtils;

@Deprecated
@Profile("prod")
@Configuration
public class InstallationSeedProviderInitializationValidationProcedureConfigurationProd {

    @Autowired
    public void init(InstallationSeedProvider provider) {
        String key = provider.get();
        if (ObjectUtils.isEmpty(key)) {
            throw new RuntimeException("InstallationSeedProvider key must not be empty");
        }
    }
}

package com.evolution.dropfilecli.command.config;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.configuration.app.DropFileAppConfig;
import com.evolution.dropfile.configuration.app.DropFileAppConfigManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.URI;

@Component
@CommandLine.Command(
        name = "daemon-public-address",
        description = "Set public daemon address"
)
public class DaemonConfigSetPublicAddressCommand implements Runnable {

    private final DropFileAppConfigManager appConfigManager;

    @CommandLine.Parameters(index = "0", description = "Address")
    private String address;

    @Autowired
    public DaemonConfigSetPublicAddressCommand(DropFileAppConfigManager appConfigManager) {
        this.appConfigManager = appConfigManager;
    }

    @Override
    public void run() {
        URI addressURI = CommonUtils.toURI(address);
        System.out.println("Setting publicDaemonAddressURI: " + addressURI);
        DropFileAppConfig originalConfig = appConfigManager.get();
        DropFileAppConfig.DropFileDaemonAppConfig originalDaemonAppConfig = originalConfig.getDaemonAppConfig();

        DropFileAppConfig.DropFileDaemonAppConfig newDaemonAppConfig = new DropFileAppConfig.DropFileDaemonAppConfig(
                originalDaemonAppConfig.getDownloadDirectory(),
                originalDaemonAppConfig.getDaemonPort(),
                addressURI
        );
        DropFileAppConfig newConfig = new DropFileAppConfig(
                originalConfig.getCliAppConfig(),
                newDaemonAppConfig
        );
        appConfigManager.save(newConfig);
        System.out.println("OK");
    }
}

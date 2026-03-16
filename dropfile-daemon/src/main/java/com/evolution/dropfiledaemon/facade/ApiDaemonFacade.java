package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.dto.DaemonInfoResponseDTO;
import com.evolution.dropfiledaemon.DropFileDaemonApplication;
import com.evolution.dropfiledaemon.configuration.ApplicationConfigStore;
import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import com.evolution.dropfiledaemon.system.SystemInfoProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ApiDaemonFacade {

    private final ApplicationConfigStore applicationConfigStore;

    private final SystemInfoProvider systemInfoProvider;

    private final DaemonApplicationProperties daemonApplicationProperties;

    public DaemonInfoResponseDTO info() {
        return new DaemonInfoResponseDTO(
                systemInfoProvider.getSystemInfo(),
                daemonApplicationProperties
        );
    }

    public void shutdown() {
        DropFileDaemonApplication.exit();
    }

    public void cacheReset() {
        applicationConfigStore.cacheReset();
    }
}

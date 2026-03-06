package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.dto.DaemonInfoResponseDTO;
import com.evolution.dropfiledaemon.DropFileDaemonApplication;
import com.evolution.dropfiledaemon.configuration.ApplicationConfigStore;
import com.evolution.dropfiledaemon.system.SystemInfoProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ApiDaemonFacade {

    private final ApplicationConfigStore applicationConfigStore;

    private final SystemInfoProvider systemInfoProvider;

    public DaemonInfoResponseDTO info() {
        return new DaemonInfoResponseDTO(
                systemInfoProvider.getSystemInfo(),
                applicationConfigStore.getDaemonAppConfigStore().getRequired()
        );
    }

    public void shutdown() {
        DropFileDaemonApplication.exit();
    }

    public void cacheReset() {
        applicationConfigStore.cacheReset();
    }
}

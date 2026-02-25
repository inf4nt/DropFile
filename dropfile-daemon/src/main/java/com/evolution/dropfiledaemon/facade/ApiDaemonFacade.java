package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.dto.DaemonInfoResponseDTO;
import com.evolution.dropfiledaemon.configuration.ApplicationConfigStore;
import com.evolution.dropfiledaemon.system.SystemInfoProvider;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;

@RequiredArgsConstructor
@Component
public class ApiDaemonFacade {

    private final ApplicationConfigStore applicationConfigStore;

    private final SystemInfoProvider systemInfoProvider;

    private final ApplicationContext applicationContext;

    public void shutdown() {
        Executors.newVirtualThreadPerTaskExecutor().execute(() -> {
            SpringApplication.exit(applicationContext, () -> 0);
        });
    }

    @SneakyThrows
    public DaemonInfoResponseDTO info() {
        return new DaemonInfoResponseDTO(
                systemInfoProvider.getSystemInfo(),
                applicationConfigStore.getAppConfigStore().getRequired().daemonAppConfig()
        );
    }
}

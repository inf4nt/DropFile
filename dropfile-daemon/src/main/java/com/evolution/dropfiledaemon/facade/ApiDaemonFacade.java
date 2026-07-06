package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.SystemInfoProvider;
import com.evolution.dropfile.common.dto.DaemonInfoResponseDTO;
import com.evolution.dropfiledaemon.DropFileDaemonApplication;
import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.util.Map;

@RequiredArgsConstructor
@Component
public class ApiDaemonFacade {

    private final CacheResetFacade cacheResetFacade;

    private final SystemInfoProvider systemInfoProvider;

    private final DaemonApplicationProperties daemonApplicationProperties;

    private final ObjectMapper objectMapper;

    @SneakyThrows
    public DaemonInfoResponseDTO info() {
        String json = objectMapper.writeValueAsString(daemonApplicationProperties);
        Map<String, String> daemonProperties = objectMapper.readValue(json, new TypeReference<>() {});
        return new DaemonInfoResponseDTO(
                systemInfoProvider.getSystemInfo(),
                daemonProperties
        );
    }

    public void shutdown() {
        DropFileDaemonApplication.exit();
    }

    public void cacheReset() {
        cacheResetFacade.reset();
    }
}

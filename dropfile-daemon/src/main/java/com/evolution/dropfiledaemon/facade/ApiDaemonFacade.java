package com.evolution.dropfiledaemon.facade;

import com.evolution.dropfile.common.SystemInfoProvider;
import com.evolution.dropfile.common.dto.DaemonInfoResponseDTO;
import com.evolution.dropfiledaemon.DropFileDaemonApplication;
import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import com.evolution.dropfiledaemon.service.InetLocalAddressService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class ApiDaemonFacade {

    private final CacheResetFacade cacheResetFacade;

    private final GarbageCollectorFacade garbageCollectorFacade;

    private final SystemInfoProvider systemInfoProvider;

    private final DaemonApplicationProperties daemonApplicationProperties;

    private final ObjectMapper objectMapper;

    private final InetLocalAddressService inetLocalAddressService;

    public DaemonInfoResponseDTO info() throws IOException {
        Map<String, String> daemonProperties = objectMapper.readValue(
                objectMapper.writeValueAsString(daemonApplicationProperties), new TypeReference<>() {
                });

        Map<String, List<Map<String, String>>> interfaces = objectMapper.readValue(
                objectMapper.writeValueAsString(inetLocalAddressService.getConnectionAddress()),
                new TypeReference<>() {
                });

        return new DaemonInfoResponseDTO(
                systemInfoProvider.getSystemInfo(),
                daemonProperties,
                interfaces
        );
    }

    public void shutdown() {
        DropFileDaemonApplication.exit();
    }

    public void cacheReset() {
        cacheResetFacade.reset();
    }

    public void garbageCollector() {
        garbageCollectorFacade.purge();
    }
}

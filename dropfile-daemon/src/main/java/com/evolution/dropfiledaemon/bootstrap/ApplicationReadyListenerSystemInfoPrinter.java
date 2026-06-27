package com.evolution.dropfiledaemon.bootstrap;

import com.evolution.dropfiledaemon.bootstrap.event.DropFileDaemonApplicationReadyEvent;
import com.evolution.dropfiledaemon.configuration.DaemonApplicationProperties;
import com.evolution.dropfiledaemon.system.SystemInfoProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Component
public class ApplicationReadyListenerSystemInfoPrinter {

    private final SystemInfoProvider systemInfoProvider;

    private final DaemonApplicationProperties daemonApplicationProperties;

    private final ObjectMapper objectMapper;

    @SneakyThrows
    @EventListener(DropFileDaemonApplicationReadyEvent.class)
    public void onApplicationEvent() {
        log.info("DropFile daemon initialization completed and ready to go");

        Map<String, Object> systemInfo = systemInfoProvider.getSystemInfo();
        System.out.println("================================");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(
                systemInfo
        ));
        System.out.println("================================");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(
                daemonApplicationProperties
        ));
        System.out.println("================================");
    }
}

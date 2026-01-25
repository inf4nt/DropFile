package com.evolution.dropfiledaemon;

import com.evolution.dropfile.store.app.AppConfigStore;
import com.evolution.dropfiledaemon.system.SystemInfoProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class BootstrapSystemInfoPrinter {

    private final SystemInfoProvider systemInfoProvider;

    private final AppConfigStore appConfigStore;

    private final ObjectMapper objectMapper;

    @Autowired
    public BootstrapSystemInfoPrinter(SystemInfoProvider systemInfoProvider,
                                      AppConfigStore appConfigStore,
                                      ObjectMapper objectMapper) {
        this.systemInfoProvider = systemInfoProvider;
        this.appConfigStore = appConfigStore;
        this.objectMapper = objectMapper;
    }

    @SneakyThrows
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationEvent() {
        Map<String, Object> systemInfo = systemInfoProvider.getSystemInfo();
        System.out.println("================================");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(
                systemInfo
        ));
        System.out.println("================================");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(
                appConfigStore.getRequired().daemonAppConfig()
        ));
        System.out.println("================================");
    }
}

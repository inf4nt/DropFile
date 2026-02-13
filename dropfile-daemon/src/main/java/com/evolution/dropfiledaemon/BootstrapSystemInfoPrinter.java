package com.evolution.dropfiledaemon;

import com.evolution.dropfiledaemon.configuration.ApplicationConfigStore;
import com.evolution.dropfiledaemon.system.SystemInfoProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@RequiredArgsConstructor
@Component
public class BootstrapSystemInfoPrinter {

    private final SystemInfoProvider systemInfoProvider;

    private final ApplicationConfigStore applicationConfigStore;

    private final ObjectMapper objectMapper;

    @SneakyThrows
    @EventListener(ApplicationConfigStore.ApplicationConfigStoreInitialized.class)
    public void onApplicationEvent() {
        Map<String, Object> systemInfo = systemInfoProvider.getSystemInfo();
        System.out.println("================================");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(
                systemInfo
        ));
        System.out.println("================================");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(
                applicationConfigStore.getAppConfigStore().getRequired().daemonAppConfig()
        ));
        System.out.println("================================");
    }
}

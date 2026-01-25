package com.evolution.dropfiledaemon;

import com.evolution.dropfiledaemon.system.SystemInfoProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class BootstrapSystemInfoPrinter
        implements ApplicationListener<ApplicationReadyEvent> {

    private final SystemInfoProvider systemInfoProvider;

    @Autowired
    public BootstrapSystemInfoPrinter(SystemInfoProvider systemInfoProvider) {
        this.systemInfoProvider = systemInfoProvider;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        Map<String, Object> systemInfo = systemInfoProvider.getSystemInfo();
        for (Map.Entry<String, Object> entry : systemInfo.entrySet()) {
            System.out.println("================================");
            System.out.println(entry.getKey());
            System.out.println(entry.getValue());
        }
        System.out.println("================================");
    }
}

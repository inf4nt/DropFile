package com.evolution.dropfiledaemon.system;

import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class SystemInfoProvider {

    public Map<String, Object> getSystemInfo() {
        return new LinkedHashMap<>() {{
            put("Runtime.getRuntime().availableProcessors()", Runtime.getRuntime().availableProcessors());
            put("ManagementFactory.getMemoryMXBean().getHeapMemoryUsage()", ManagementFactory.getMemoryMXBean().getHeapMemoryUsage());
            put("ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage()", ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage());

            long maxMemory = Runtime.getRuntime().maxMemory();
            put("Runtime.getRuntime().maxMemory()", maxMemory + " bytes (" + toMb(maxMemory) + " MB)");
            long totalMemory = Runtime.getRuntime().totalMemory();
            put("Runtime.getRuntime().totalMemory()", totalMemory + " bytes (" + toMb(totalMemory) + " MB)");
            long freeMemory = Runtime.getRuntime().freeMemory();
            put("Runtime.getRuntime().freeMemory()", freeMemory + " bytes (" + toMb(freeMemory) + " MB)");
        }};
    }

    private long toMb(long value) {
        return value / 1_024 / 1_024;
    }
}

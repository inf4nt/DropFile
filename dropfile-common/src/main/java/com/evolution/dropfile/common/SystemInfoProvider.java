package com.evolution.dropfile.common;

import java.lang.management.ManagementFactory;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class SystemInfoProvider {

    private static final String PROCESS_ID = UUID.randomUUID().toString();

    public Map<String, String> getSystemInfo() {
        return new LinkedHashMap<>() {{
            put("SystemInfoProvider.PROCESS_ID", PROCESS_ID);
            put("Runtime.getRuntime().availableProcessors()", String.valueOf(Runtime.getRuntime().availableProcessors()));
            put("ManagementFactory.getMemoryMXBean().getHeapMemoryUsage()", ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().toString());
            put("ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage()", ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().toString());

            long maxMemory = Runtime.getRuntime().maxMemory();
            put("Runtime.getRuntime().maxMemory()", maxMemory + " bytes (" + CommonUtils.toDisplaySize(maxMemory) + ")");
            long totalMemory = Runtime.getRuntime().totalMemory();
            put("Runtime.getRuntime().totalMemory()", totalMemory + " bytes (" + CommonUtils.toDisplaySize(totalMemory) + ")");
            long freeMemory = Runtime.getRuntime().freeMemory();
            put("Runtime.getRuntime().freeMemory()", freeMemory + " bytes (" + CommonUtils.toDisplaySize(freeMemory) + ")");
        }};
    }
}

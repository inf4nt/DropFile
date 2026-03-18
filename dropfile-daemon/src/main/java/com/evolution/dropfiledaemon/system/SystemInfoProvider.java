package com.evolution.dropfiledaemon.system;

import com.evolution.dropfile.common.CommonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@Component
public class SystemInfoProvider {

    private static final String PROCESS_ID = UUID.randomUUID().toString();

    public Map<String, Object> getSystemInfo() {
        return new LinkedHashMap<>() {{
            put("SystemInfoProvider.PROCESS_ID", PROCESS_ID);
            put("Runtime.getRuntime().availableProcessors()", Runtime.getRuntime().availableProcessors());
            put("ManagementFactory.getMemoryMXBean().getHeapMemoryUsage()", ManagementFactory.getMemoryMXBean().getHeapMemoryUsage());
            put("ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage()", ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage());

            long maxMemory = Runtime.getRuntime().maxMemory();
            put("Runtime.getRuntime().maxMemory()", maxMemory + " bytes (" + CommonUtils.toDisplaySize(maxMemory) + ")");
            long totalMemory = Runtime.getRuntime().totalMemory();
            put("Runtime.getRuntime().totalMemory()", totalMemory + " bytes (" + CommonUtils.toDisplaySize(totalMemory) + ")");
            long freeMemory = Runtime.getRuntime().freeMemory();
            put("Runtime.getRuntime().freeMemory()", freeMemory + " bytes (" + CommonUtils.toDisplaySize(freeMemory) + ")");
        }};
    }
}

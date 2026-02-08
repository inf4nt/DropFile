package com.evolution.dropfiledaemon.system;

import com.evolution.dropfiledaemon.util.FileHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.util.LinkedHashMap;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class SystemInfoProvider {

    private final FileHelper fileHelper;

    public Map<String, Object> getSystemInfo() {
        return new LinkedHashMap<>() {{
            put("Runtime.getRuntime().availableProcessors()", Runtime.getRuntime().availableProcessors());
            put("ManagementFactory.getMemoryMXBean().getHeapMemoryUsage()", ManagementFactory.getMemoryMXBean().getHeapMemoryUsage());
            put("ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage()", ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage());

            long maxMemory = Runtime.getRuntime().maxMemory();
            put("Runtime.getRuntime().maxMemory()", maxMemory + " bytes (" + fileHelper.toDisplaySize(maxMemory) + ")");
            long totalMemory = Runtime.getRuntime().totalMemory();
            put("Runtime.getRuntime().totalMemory()", totalMemory + " bytes (" + fileHelper.toDisplaySize(totalMemory) + ")");
            long freeMemory = Runtime.getRuntime().freeMemory();
            put("Runtime.getRuntime().freeMemory()", freeMemory + " bytes (" + fileHelper.toDisplaySize(freeMemory) + ")");
        }};
    }
}

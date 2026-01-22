//package com.evolution.dropfiledaemon.controller;
//
//import org.springframework.boot.context.event.ApplicationReadyEvent;
//import org.springframework.context.event.EventListener;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.lang.management.ManagementFactory;
//import java.util.LinkedHashMap;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/system/info")
//public class SystemInfoRestController {
//
//    @EventListener(ApplicationReadyEvent.class)
//    public void ready() {
//        Map<String, Object> systemInfo = getSystemInfo();
//        for (Map.Entry<String, Object> entry : systemInfo.entrySet()) {
//            System.out.println("================================");
//            System.out.println(entry.getKey());
//            System.out.println(entry.getValue());
//        }
//        System.out.println("================================");
//    }
//
//    @GetMapping
//    public Map<String, Object> getSystemInfo() {
//        return new LinkedHashMap<>() {{
//            put("Runtime.getRuntime().availableProcessors()", Runtime.getRuntime().availableProcessors());
//            put("ManagementFactory.getMemoryMXBean().getHeapMemoryUsage()", ManagementFactory.getMemoryMXBean().getHeapMemoryUsage());
//            put("ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage()", ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage());
//
//            long maxMemory = Runtime.getRuntime().maxMemory();
//            put("Runtime.getRuntime().maxMemory()", maxMemory + " bytes (" + (maxMemory / 1024 / 1024) + " MB)");
//            long totalMemory = Runtime.getRuntime().totalMemory();
//            put("Runtime.getRuntime().totalMemory()", totalMemory + " bytes (" + (totalMemory / 1024 / 1024) + " MB)");
//            long freeMemory = Runtime.getRuntime().freeMemory();
//            put("Runtime.getRuntime().freeMemory()", freeMemory + " bytes (" + (freeMemory / 1024 / 1024) + " MB)");
//        }};
//    }
//}

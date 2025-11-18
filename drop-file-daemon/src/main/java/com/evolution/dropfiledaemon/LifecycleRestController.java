package com.evolution.dropfiledaemon;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.Executors;

@RestController
@RequestMapping("/daemon")
public class LifecycleRestController {

    @PostMapping("/shutdown")
    public void shutdown() {
        Executors.newSingleThreadExecutor()
                .submit(() -> {
                    System.exit(0);
                });
    }

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }
}

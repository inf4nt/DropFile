package com.evolution.dropfiledaemon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
public class DropFileDaemonApplication {

    private static ConfigurableApplicationContext context;

    public static void main(String[] args) {
        context = SpringApplication.run(DropFileDaemonApplication.class, args);
    }

    public static void exit() {
        Thread.ofVirtual().start(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException _) {
            }
            int exit = SpringApplication.exit(context, () -> 0);
            System.exit(exit);
        });
    }
}

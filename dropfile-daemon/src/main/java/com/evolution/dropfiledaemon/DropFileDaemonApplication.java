package com.evolution.dropfiledaemon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
public class DropFileDaemonApplication {

    public static void main(String[] args) {
        SpringApplication.run(DropFileDaemonApplication.class, args);
    }
}

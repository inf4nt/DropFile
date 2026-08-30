package com.evolution.dropfiledaemon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.File;
import java.io.IOException;

@EnableScheduling
@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
public class DropFileDaemonApplication {

    private static ConfigurableApplicationContext context;

    public static void main(String[] args) {
        startElectronUI();

        context = SpringApplication.run(DropFileDaemonApplication.class, args);
    }

    // TODO the build requires SUDO or 'Run as administrator'. mvn clean install

    private static void startElectronUI() {
        new Thread(() -> {
            try {
                File prodExe = new File("dropfile-daemon/electron-ui/dist/dropfile-ui 1.0.0.exe");

                System.out.println(prodExe.getAbsolutePath());

                ProcessBuilder pb;
                if (prodExe.exists()) {
                    System.out.println("[SYSTEM] Starting Production Electron UI...");
                    pb = new ProcessBuilder(prodExe.getAbsolutePath());
                } else {
                    System.out.println("[SYSTEM] Starting Dev Electron UI...");

                    String os = System.getProperty("os.name").toLowerCase();
                    String electronCmd = os.contains("win")
                            ? "electron-ui/node_modules/.bin/electron.cmd"
                            : "electron-ui/node_modules/.bin/electron";

                    pb = new ProcessBuilder(electronCmd, "electron-ui");
                }

                pb.inheritIO();
                pb.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
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
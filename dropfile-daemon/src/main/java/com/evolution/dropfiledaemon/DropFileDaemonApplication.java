package com.evolution.dropfiledaemon;

import com.evolution.dropfiledaemon.bootstrap.DropFileDaemonApplicationReadyEvent;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

@EnableScheduling
@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
public class DropFileDaemonApplication {

    private static ConfigurableApplicationContext context;

    private static ModernQrDropApp guiApp;

    public static void main(String[] args) {
        System.out.println("Add argument swing or elector to execute. By default swing");
        if (Arrays.stream(args).anyMatch(it -> it.contains("swing"))) {
            runSwing();
        } else if(Arrays.stream(args).anyMatch(it -> it.contains("electron"))) {
            startElectronUI();
        } else {
            runSwing();
        }

        context = SpringApplication.run(DropFileDaemonApplication.class, args);
    }

    // TODO the ELECTRON build requires SUDO or 'Run as administrator'. mvn clean install

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

    private static void runSwing() {
        System.setProperty("java.awt.headless", "false");

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            guiApp = new ModernQrDropApp();
            guiApp.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            guiApp.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent e) {
                    if (context != null) {
                        SpringApplication.exit(context, () -> 0);
                    }
                }
            });
            guiApp.setVisible(true);
        });
    }

    @EventListener(DropFileDaemonApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (guiApp != null) {
            guiApp.setLoading(false);
        }
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
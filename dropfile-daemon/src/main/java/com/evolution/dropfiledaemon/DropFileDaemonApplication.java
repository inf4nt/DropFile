package com.evolution.dropfiledaemon;

import com.evolution.dropfiledaemon.bootstrap.DropFileDaemonApplicationReadyEvent;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.swing.*;

@EnableScheduling
@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
public class DropFileDaemonApplication {

    private static ConfigurableApplicationContext context;

    private static ModernQrDropApp guiApp;

    public static void main(String[] args) {
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

        context = SpringApplication.run(DropFileDaemonApplication.class, args);
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

package com.evolution.dropfilecli.command.daemon;

import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.io.File;

@Component
@CommandLine.Command(
        name = "start",
        description = "start daemon"
)
public class DaemonStartCommand implements Runnable {

    @SneakyThrows
    @Override
    public void run() {
        String daemonHome = System.getenv("DROPFILE_DAEMON_HOME");
        if (daemonHome == null) {
            System.err.println("DROPFILE_DAEMON_HOME has not set");
            return;
        }

        File exeFile = new File(daemonHome, "DropFileDaemon.exe");
        if (!exeFile.exists()) {
            System.err.println("No found DropFileDaemon.exe " + daemonHome);
            return;
        }

        new ProcessBuilder(exeFile.getAbsolutePath())
//                .inheritIO()
                .start();

        System.out.println("Executed");
    }
}

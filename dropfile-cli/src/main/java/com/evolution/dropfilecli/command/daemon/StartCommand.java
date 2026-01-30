package com.evolution.dropfilecli.command.daemon;

import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@CommandLine.Command(
        name = "start",
        description = "Daemon start"
)
public class StartCommand implements Runnable {

    private static final String DROPFILE_HOME = "DROPFILE_HOME";

    private static final String DROPFILE_DAEMON_EXECUTABLE = "dropfile-daemon.bat";

    @Override
    public void run() {
        String appHome = System.getenv(DROPFILE_HOME);

        if (appHome == null || appHome.isBlank()) {
            throw new IllegalStateException("ENV DROPFILE_HOME is not set");
        }
        Path executablePath = Paths.get(appHome, "bin", DROPFILE_DAEMON_EXECUTABLE);
        if (Files.notExists(executablePath)) {
            throw new IllegalStateException("No executable file found: " + executablePath);
        }

        startProcess(executablePath);
    }

    @SneakyThrows
    private void startProcess(Path executablePath) {
        ProcessBuilder processBuilder = new ProcessBuilder(executablePath.toString());
        processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD);
        processBuilder.start();
    }
}
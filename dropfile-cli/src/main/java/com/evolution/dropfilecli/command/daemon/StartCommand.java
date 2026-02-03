package com.evolution.dropfilecli.command.daemon;

import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@CommandLine.Command(
        name = "start",
        description = "Daemon start"
)
public class StartCommand implements Runnable {

    @Override
    public void run() {
        Path binPath = getBinPath();
        Path executable = isWindows() ? binPath.resolve("dropfile-daemon.cmd")
                : binPath.resolve("dropfile-daemon");
        execute(executable);
    }

    private boolean isWindows() {
        String os = System.getProperty("os.name");
        return os.toLowerCase().contains("windows");
    }

    private Path getBinPath() {
        String cmd = System.getProperty("sun.java.command");
        String jar = cmd.split(" ")[0];
        Path jarPath = Paths.get(jar);
        Path parent = jarPath.getParent().getParent();
        return parent.resolve("bin");
    }

    @SneakyThrows
    private void execute(Path executablePath) {
        ProcessBuilder processBuilder = new ProcessBuilder(executablePath.toString());
        processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD);
        processBuilder.start();
    }
}
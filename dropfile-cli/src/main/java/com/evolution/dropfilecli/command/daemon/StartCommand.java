package com.evolution.dropfilecli.command.daemon;

import com.evolution.dropfilecli.SimpleCommandHandler;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

@Component
@CommandLine.Command(
        name = "start",
        description = "Daemon start"
)
public class StartCommand implements SimpleCommandHandler {

    @Override
    public void handle() {
        Path binPath = getBinPath();
        Path executable = isWindows() ? binPath.resolve("dropfile-daemon.cmd")
                : binPath.resolve("dropfile-daemon");
        if (Files.notExists(executable)) {
            throw new RuntimeException(new FileNotFoundException(executable.toAbsolutePath().toString()));
        }
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
        System.out.println("Executing " + executablePath.toString());
        ProcessBuilder pb = new ProcessBuilder(executablePath.toString());

        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        pb.redirectError(ProcessBuilder.Redirect.DISCARD);

        Process process = pb.start();

        boolean exited = process.waitFor(Duration.ofSeconds(5));

        if (!exited) {
            System.out.println("Command completed successfully. To get daemon execution status execute $dropfile daemon status");
        } else {
            System.out.println("Process exited with code " + process.exitValue());
        }
    }
}
package com.evolution.dropfilecli.command.daemon;

import com.evolution.dropfilecli.command.AbstractCommandHandler;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

@Component
@CommandLine.Command(
        name = "start",
        description = "Daemon start",
        customSynopsis = "dropfile daemon start",
        parameterListHeading = "%nRequired parameters:%n",
        optionListHeading = "%nOptional parameters:%n"
)
public class StartCommand extends AbstractCommandHandler {

    @Override
    public void handle() throws Exception {
        Path executable = getDaemonExecutablePath();

        if (Files.notExists(executable)) {
            throw new FileNotFoundException(
                    "Daemon start failed. Executable file not found: %s".formatted(executable.toAbsolutePath())
            );
        }

        execute(executable);
    }

    private Path getDaemonExecutablePath() {
        String appHome = System.getProperty("dropfile.home");
        if (appHome == null || appHome.isBlank()) {
            throw new IllegalStateException("System property 'dropfile.home' is not set");
        }

        String executableName = isWindows() ? "dropfile-daemon.cmd" : "dropfile-daemon";
        return Paths.get(appHome, "bin", executableName).toAbsolutePath().normalize();
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    private void execute(Path executablePath) throws IOException, InterruptedException {
        System.out.println("Executing " + executablePath);
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
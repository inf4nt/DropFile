package com.evolution.dropfilecli.command.daemon;

import com.evolution.dropfilecli.client.DaemonClient;
import com.evolution.dropfilecli.command.AbstractCommandHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

@RequiredArgsConstructor
@Component
@CommandLine.Command(
        name = "start",
        description = "Daemon start",
        customSynopsis = "dropfile daemon start",
        parameterListHeading = "%nRequired parameters:%n",
        optionListHeading = "%nOptional parameters:%n"
)
public class StartCommand extends AbstractCommandHandler {

    private final DaemonClient daemonClient;

    @Override
    public void handle() throws Exception {
        if (isDaemonReachable()) {
            System.out.println("Daemon already running");
            return;
        }

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

    private boolean isDaemonReachable() {
        try {
            HttpResponse<byte[]> httpResponse = daemonClient.daemonPing();
            return httpResponse.statusCode() == 200;
        } catch (Exception _) {
            return false;
        }
    }
}
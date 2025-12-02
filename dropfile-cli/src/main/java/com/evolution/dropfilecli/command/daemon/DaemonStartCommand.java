package com.evolution.dropfilecli.command.daemon;

import com.evolution.dropfilecli.CommandHttpHandler;
import com.evolution.dropfilecli.client.DaemonClient;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.io.File;
import java.net.ConnectException;
import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "start",
        description = "start daemon"
)
public class DaemonStartCommand implements CommandHttpHandler<Void> {

    private final DaemonClient daemonClient;

    @Autowired
    public DaemonStartCommand(DaemonClient daemonClient) {
        this.daemonClient = daemonClient;
    }

    @Override
    public HttpResponse<Void> execute() {
        return daemonClient.pingDaemon();
    }

    @Override
    public void handleSuccessful(HttpResponse<Void> response) {
        System.out.println("Daemon is running");
    }

    @Override
    public void handleError(Exception exception) {
        if (exception instanceof ConnectException) {
            runDaemon();
        }
    }

    @SneakyThrows
    private void runDaemon() {
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

        System.out.println("Started");
    }
}

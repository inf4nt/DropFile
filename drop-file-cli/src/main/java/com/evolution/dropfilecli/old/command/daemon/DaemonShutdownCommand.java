package com.evolution.dropfilecli.old.command.daemon;

import com.evolution.dropfilecli.old.client.DaemonHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        name = "shutdown",
        description = "Daemon shutdown"
)
public class DaemonShutdownCommand implements Runnable {

    private final DaemonHttpClient daemonHttpClient;

    @Autowired
    public DaemonShutdownCommand(DaemonHttpClient daemonHttpClient) {
        this.daemonHttpClient = daemonHttpClient;
    }

    @Override
    public void run() {
        daemonHttpClient.shutdown();
    }
}

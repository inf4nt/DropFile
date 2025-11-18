package com.evolution.dropfilecli.command.daemon;

import com.evolution.dropfilecli.client.DaemonHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        name = "shutdown",
        description = "Daemon shutdown command"
)
public class DaemonShutdownCommand implements Runnable {

    private final DaemonHttpClient daemonHttpClient;

    @Autowired
    public DaemonShutdownCommand(DaemonHttpClient daemonHttpClient) {
        this.daemonHttpClient = daemonHttpClient;
    }

    @Override
    public void run() {
        Integer statusCode = daemonHttpClient.shutdown().statusCode();
        System.out.println(statusCode);
    }
}

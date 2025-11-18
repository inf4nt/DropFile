package com.evolution.dropfilecli.command.daemon;

import com.evolution.dropfilecli.client.DaemonHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        name = "ping",
        description = "Daemon ping command"
)
public class DaemonPingCommand implements Runnable {

    private final DaemonHttpClient daemonHttpClient;

    @Autowired
    public DaemonPingCommand(DaemonHttpClient daemonHttpClient) {
        this.daemonHttpClient = daemonHttpClient;
    }

    @Override
    public void run() {
        String responseBody = daemonHttpClient.ping().body();
        System.out.println(responseBody);
    }
}

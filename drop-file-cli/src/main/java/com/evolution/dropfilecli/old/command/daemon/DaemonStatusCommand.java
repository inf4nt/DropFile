package com.evolution.dropfilecli.old.command.daemon;

import com.evolution.dropfilecli.old.client.DaemonHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "status",
        description = "Daemon status"
)
public class DaemonStatusCommand implements Runnable {

    private final DaemonHttpClient daemonHttpClient;

    @Autowired
    public DaemonStatusCommand(DaemonHttpClient daemonHttpClient) {
        this.daemonHttpClient = daemonHttpClient;
    }

    @Override
    public void run() {
        HttpResponse<String> httpResponse = daemonHttpClient.ping();
        System.out.println("Daemon status: " + httpResponse.statusCode());
    }
}

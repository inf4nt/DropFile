package com.evolution.dropfilecli.old.command.connection;

import com.evolution.dropfilecli.old.client.DaemonHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "status",
        description = "Connection status"
)
public class ConnectionStatusCommand implements Runnable {

    private final DaemonHttpClient daemonHttpClient;

    @Autowired
    public ConnectionStatusCommand(DaemonHttpClient daemonHttpClient) {
        this.daemonHttpClient = daemonHttpClient;
    }

    @Override
    public void run() {
        HttpResponse<String> httpResponse = daemonHttpClient.status();
        String connectedTo = httpResponse.body();
        if (ObjectUtils.isEmpty(connectedTo)) {
            System.out.println("No connections");
        } else {
            System.out.println("Connected to " + connectedTo);
        }
    }
}

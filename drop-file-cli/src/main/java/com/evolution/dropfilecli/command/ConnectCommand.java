package com.evolution.dropfilecli.command;

import com.evolution.dropfilecli.client.DaemonHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.URI;
import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "connect",
        description = "Connect to remote peer"
)
public class ConnectCommand implements Runnable {

    private final DaemonHttpClient daemonHttpClient;

    @CommandLine.Parameters(index = "0", description = "IP address")
    private String ip;

    @Autowired
    public ConnectCommand(DaemonHttpClient daemonHttpClient) {
        this.daemonHttpClient = daemonHttpClient;
    }

    @Override
    public void run() {
        System.out.println("Connecting to " + ip);
        URI uri = URI.create(ip);
        HttpResponse<Void> connect = daemonHttpClient.connect(uri);
        System.out.println("Connect status " + connect.statusCode());
    }
}

package com.evolution.dropfilecli.command;

import com.evolution.dropfilecli.client.DaemonHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.URI;
import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "disconnect",
        description = "Disconnect"
)
public class DisconnectCommand implements Runnable {

    private final DaemonHttpClient daemonHttpClient;

    @Autowired
    public DisconnectCommand(DaemonHttpClient daemonHttpClient) {
        this.daemonHttpClient = daemonHttpClient;
    }

    @Override
    public void run() {
        HttpResponse<Void> connect = daemonHttpClient.disconnect();
        System.out.println("Disconnect status " + connect.statusCode());
    }
}

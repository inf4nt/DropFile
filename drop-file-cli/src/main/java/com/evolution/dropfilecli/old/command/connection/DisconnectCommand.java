package com.evolution.dropfilecli.old.command.connection;

import com.evolution.dropfilecli.old.client.DaemonHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

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
        HttpResponse<Void> httpResponse = daemonHttpClient.disconnect();
        System.out.println("Disconnect status " + httpResponse.statusCode());
    }
}

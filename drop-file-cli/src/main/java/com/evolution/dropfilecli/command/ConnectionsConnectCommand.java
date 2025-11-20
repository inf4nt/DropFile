package com.evolution.dropfilecli.command;

import com.evolution.dropfilecli.client.DaemonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "connect",
        description = "Connect to remote address"
)
public class ConnectionsConnectCommand implements Runnable {

    @CommandLine.Parameters(index = "0", description = "Address")
    private String address;

    private final DaemonClient daemonClient;

    @Autowired
    public ConnectionsConnectCommand(DaemonClient daemonClient) {
        this.daemonClient = daemonClient;
    }

    @Override
    public void run() {
        System.out.println("Connecting...");
        HttpResponse<String> connectHttpResponse = daemonClient.connect(address);
        if (connectHttpResponse.statusCode() == 200) {
            System.out.println("Connected: " + connectHttpResponse.body());
        } else {
            System.out.println("Failed to connect to remote address");
            System.out.println("Status code: " + connectHttpResponse.statusCode());
            System.out.println("Body: " + connectHttpResponse.body());
        }
    }
}

package com.evolution.dropfilecli.command;

import com.evolution.dropfilecli.CommandHttpHandler;
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
public class ConnectionsConnectCommand implements CommandHttpHandler<String> {

    @CommandLine.Parameters(index = "0", description = "Address")
    private String address;

    private final DaemonClient daemonClient;

    @Autowired
    public ConnectionsConnectCommand(DaemonClient daemonClient) {
        this.daemonClient = daemonClient;
    }

    @Override
    public HttpResponse<String> execute() {
        return daemonClient.connect(address);
    }

    @Override
    public void handleSuccessful(HttpResponse<String> response) {
        System.out.println("Connected: " + response.body());
    }
}

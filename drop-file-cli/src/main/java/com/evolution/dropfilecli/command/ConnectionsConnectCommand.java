package com.evolution.dropfilecli.command;

import com.evolution.dropfile.common.dto.ConnectionsConnectionResultDTO;
import com.evolution.dropfilecli.client.DaemonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

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
        ConnectionsConnectionResultDTO result = daemonClient.connect(address);
        if (result != null) {
            System.out.println("Connected id: " + result.getConnectionId());
            System.out.println("Connect address: " + result.getConnectionAddress());
        } else {
            System.out.println("Failed to connect to remote address");
        }
    }
}

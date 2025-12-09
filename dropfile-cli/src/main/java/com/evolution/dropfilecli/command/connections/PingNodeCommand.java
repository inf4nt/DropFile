package com.evolution.dropfilecli.command.connections;


import com.evolution.dropfilecli.CommandHttpHandler;
import com.evolution.dropfilecli.client.DaemonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "ping",
        description = "Check ready-to-use trusted-out connection"
)
public class PingNodeCommand implements CommandHttpHandler<String> {

    @CommandLine.Parameters(index = "0", description = "fingerprint")
    private String fingerprint;

    private final DaemonClient daemonClient;

    @Autowired
    public PingNodeCommand(DaemonClient daemonClient) {
        this.daemonClient = daemonClient;
    }

    @Override
    public HttpResponse<String> execute() throws Exception {
        return daemonClient.nodePing(fingerprint);
    }

    @Override
    public void handleSuccessful(HttpResponse<String> response) throws Exception {
        System.out.println("Ready-to-use: " + fingerprint);
    }
}

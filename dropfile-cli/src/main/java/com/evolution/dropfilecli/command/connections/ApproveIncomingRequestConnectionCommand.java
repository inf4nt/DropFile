package com.evolution.dropfilecli.command.connections;

import com.evolution.dropfilecli.CommandHttpHandler;
import com.evolution.dropfilecli.client.DaemonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "approve",
        description = "Approve incoming connection request"
)
public class ApproveIncomingRequestConnectionCommand implements CommandHttpHandler<byte[]> {

    @CommandLine.Parameters(index = "0", description = "Fingerprint")
    private String fingerprint;

    private final DaemonClient daemonClient;

    @Autowired
    public ApproveIncomingRequestConnectionCommand(DaemonClient daemonClient) {
        this.daemonClient = daemonClient;
    }

    @Override
    public HttpResponse<byte[]> execute() {
        return daemonClient.trust(fingerprint);
    }

    @Override
    public void handleSuccessful(HttpResponse<byte[]> response) {
        System.out.println("Successfully approved fingerprint: " + fingerprint);
    }

    @Override
    public void handleUnsuccessful(HttpResponse<byte[]> response) {
        System.out.println("Failed to approve fingerprint: " + fingerprint);
        System.out.println("Message: " + new String(response.body()));
    }
}

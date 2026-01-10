package com.evolution.dropfilecli.command.connections;

import com.evolution.dropfilecli.CommandHttpHandler;
import com.evolution.dropfilecli.client.DaemonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "revoke",
        description = "Drop trusted-in connection"
)
public class RevokeCommand implements CommandHttpHandler<Void> {

    @CommandLine.Parameters(index = "0", description = "fingerprint")
    private String fingerprint;

    private final DaemonClient daemonClient;

    @Autowired
    public RevokeCommand(DaemonClient daemonClient) {
        this.daemonClient = daemonClient;
    }

    @Override
    public HttpResponse<Void> execute() throws Exception {
        return daemonClient.connectionsRevoke(fingerprint);
    }

    @Override
    public void handleSuccessful(HttpResponse<Void> response) throws Exception {
        System.out.println("Revoked trusted-in connection: " + fingerprint);
    }
}

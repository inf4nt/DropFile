package com.evolution.dropfilecli.command.connections;

import com.evolution.dropfilecli.CommandHttpHandler;
import com.evolution.dropfilecli.client.DaemonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "revoke",
        description = "Drop trusted-in connection"
)
public class RevokeCommand implements CommandHttpHandler<Void> {

    @CommandLine.ArgGroup(multiplicity = "1")
    private Exclusive exclusive;

    private static class Exclusive {
        @CommandLine.Option(names = {"-fingerprint", "--fingerprint"}, description = "Revoke by fingerprint")
        private String fingerprint;

        @CommandLine.Option(names = {"-all", "--all"}, description = "Revoke all")
        private boolean all;
    }

    private final DaemonClient daemonClient;

    @Autowired
    public RevokeCommand(DaemonClient daemonClient) {
        this.daemonClient = daemonClient;
    }

    @Override
    public HttpResponse<Void> execute() throws Exception {
        if (exclusive.all) {
            return daemonClient.connectionsRevokeAll();
        } else if (!ObjectUtils.isEmpty(exclusive.fingerprint)) {
            return daemonClient.connectionsRevoke(exclusive.fingerprint);
        }
        throw new RuntimeException("Revoke command cannot be executed. Check its variables");
    }

    @Override
    public void handleSuccessful(HttpResponse<Void> response) throws Exception {
        System.out.println("Revoked completed");
    }
}

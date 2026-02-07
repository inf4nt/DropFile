package com.evolution.dropfilecli.command.connections;

import com.evolution.dropfilecli.AbstractCommandHttpHandler;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "revoke",
        description = "Drop trusted-in connection"
)
public class RevokeCommand extends AbstractCommandHttpHandler {

    @CommandLine.ArgGroup(multiplicity = "1")
    private Exclusive exclusive;

    private static class Exclusive {
        @CommandLine.Option(names = {"-fingerprint", "--fingerprint"}, description = "Revoke by fingerprint")
        private String fingerprint;

        @CommandLine.Option(names = {"-all", "--all"}, description = "Revoke all")
        private boolean all;
    }

    @Override
    public HttpResponse<byte[]> execute() {
        if (exclusive.all) {
            return daemonClient.connectionsRevokeAll();
        }
        return daemonClient.connectionsRevoke(exclusive.fingerprint);
    }
}

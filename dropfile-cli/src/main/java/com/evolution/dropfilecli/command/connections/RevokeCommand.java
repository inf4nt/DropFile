package com.evolution.dropfilecli.command.connections;

import com.evolution.dropfilecli.command.AbstractCommandHttpHandler;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "revoke",
        description = "Drop trusted-in connection",
        customSynopsis = "dropfile connections revoke <fingerprint>",
        parameterListHeading = "%nRequired parameters:%n",
        optionListHeading = "%nOptional parameters:%n"
)
public class RevokeCommand extends AbstractCommandHttpHandler<Void> {

    @CommandLine.ArgGroup(multiplicity = "1")
    private Exclusive exclusive;

    private static class Exclusive {
        @CommandLine.Parameters(index = "0", description = "Revoke by fingerprint")
        private String fingerprint;

        @CommandLine.Option(names = {"--all"}, description = "Revoke all connections")
        private boolean all;
    }

    @Override
    public HttpResponse<byte[]> execute() {
        if (exclusive.all) {
            return daemonClient.handshakeRevokeAll();
        }
        return daemonClient.handshakeRevoke(exclusive.fingerprint);
    }
}

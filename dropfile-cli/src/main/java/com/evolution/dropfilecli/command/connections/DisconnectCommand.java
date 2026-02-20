package com.evolution.dropfilecli.command.connections;

import com.evolution.dropfilecli.AbstractCommandHttpHandler;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "disconnect",
        description = "Disconnect trusted-out connection"
)
public class DisconnectCommand extends AbstractCommandHttpHandler {

    @CommandLine.ArgGroup(multiplicity = "1")
    private Exclusive exclusive;

    private static class Exclusive {
        @CommandLine.Option(
                names = {"-fingerprint", "--fingerprint", "--f", "-f"},
                description = "Disconnect by fingerprint"
        )
        private String fingerprint;

        @CommandLine.Option(names = {"-current", "--current"}, description = "Disconnect current")
        private boolean current;

        @CommandLine.Option(names = {"-all", "--all"}, description = "Disconnect all")
        private boolean all;
    }

    @Override
    public HttpResponse<byte[]> execute() {
        if (exclusive.all) {
            return daemonClient.connectionsDisconnectAll();
        } else if (exclusive.current) {
            return daemonClient.connectionsDisconnectCurrent();
        }
        return daemonClient.connectionsDisconnect(exclusive.fingerprint);
    }
}

package com.evolution.dropfilecli.command.connections;

import com.evolution.dropfilecli.CommandHttpHandler;
import com.evolution.dropfilecli.client.DaemonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "disconnect",
        description = "Disconnect trusted-out connection"
)
public class DisconnectCommand implements CommandHttpHandler<byte[]> {

    @CommandLine.ArgGroup(multiplicity = "1")
    private Exclusive exclusive;

    private static class Exclusive {
        @CommandLine.Option(names = {"-fingerprint", "--fingerprint"}, description = "Disconnect by fingerprint")
        private String fingerprint;

        @CommandLine.Option(names = {"-current", "--current"}, description = "Disconnect current")
        private boolean current;

        @CommandLine.Option(names = {"-all", "--all"}, description = "Disconnect all")
        private boolean all;
    }

    private final DaemonClient daemonClient;

    @Autowired
    public DisconnectCommand(DaemonClient daemonClient) {
        this.daemonClient = daemonClient;
    }

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        if (exclusive.all) {
            return daemonClient.connectionsDisconnectAll();
        } else if (exclusive.current) {
            return daemonClient.connectionsDisconnectCurrent();
        }
        return daemonClient.connectionsDisconnect(exclusive.fingerprint);
    }
}

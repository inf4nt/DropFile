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
        name = "disconnect",
        description = "Disconnect trusted-out connection"
)
public class DisconnectCommand implements CommandHttpHandler<Void> {

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
    public HttpResponse<Void> execute() throws Exception {
        if (!ObjectUtils.isEmpty(exclusive.fingerprint)) {
            return daemonClient.connectionsDisconnect(exclusive.fingerprint);
        } else if (exclusive.all) {
            return daemonClient.connectionsDisconnectAll();
        } else if (exclusive.current) {
            return daemonClient.connectionsDisconnectCurrent();
        }
        throw new RuntimeException("Disconnect command cannot be executed. Check its variables");
    }

    @Override
    public void handleSuccessful(HttpResponse<Void> response) throws Exception {
        System.out.println("Disconnected completed");
    }
}

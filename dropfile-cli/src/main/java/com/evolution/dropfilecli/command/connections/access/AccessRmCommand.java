package com.evolution.dropfilecli.command.connections.access;

import com.evolution.dropfilecli.CommandHttpHandler;
import com.evolution.dropfilecli.client.DaemonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "rm",
        description = "rm access key"
)
public class AccessRmCommand implements CommandHttpHandler<Void> {

    @CommandLine.ArgGroup(multiplicity = "1")
    private Exclusive exclusive;

    private static class Exclusive {
        @CommandLine.Option(names = {"-id", "--id"}, description = "id")
        private String id;

        @CommandLine.Option(names = {"-all", "--all"}, description = "rm all access keys")
        private boolean all;
    }

    private final DaemonClient daemonClient;

    @Autowired
    public AccessRmCommand(DaemonClient daemonClient) {
        this.daemonClient = daemonClient;
    }

    @Override
    public HttpResponse<Void> execute() throws Exception {
        if (exclusive.all) {
            return daemonClient.rmAllAccessKeys();
        }
        return daemonClient.rmAccessKey(exclusive.id);
    }

    @Override
    public void handleSuccessful(HttpResponse<Void> response) throws Exception {
        if (exclusive.all) {
            System.out.println("Revoke all successful");
        } else {
            System.out.println("Revoke access key successful: " + exclusive.id);
        }
    }

    @Override
    public void handleUnsuccessful(HttpResponse<Void> response) throws Exception {
        if (response.statusCode() == 404) {
            System.out.println("Revoke access key not found: " + exclusive.id);
        } else {
            System.out.println("Unknown http status code: " + response.statusCode());
        }
    }
}

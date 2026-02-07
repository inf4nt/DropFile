package com.evolution.dropfilecli.command.connections.access;

import com.evolution.dropfilecli.AbstractCommandHttpHandler;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "rm",
        description = "rm access key"
)
public class AccessRmCommand extends AbstractCommandHttpHandler {

    @CommandLine.ArgGroup(multiplicity = "1")
    private Exclusive exclusive;

    private static class Exclusive {
        @CommandLine.Option(names = {"-id", "--id"}, description = "id")
        private String id;

        @CommandLine.Option(names = {"-all", "--all"}, description = "rm all access keys")
        private boolean all;
    }

    @Override
    public HttpResponse<byte[]> execute() {
        if (exclusive.all) {
            return daemonClient.connectionsAccessRmAll();
        }
        return daemonClient.connectionsAccessRm(exclusive.id);
    }
}

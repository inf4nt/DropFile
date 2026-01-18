package com.evolution.dropfilecli.command.share;

import com.evolution.dropfilecli.CommandHttpHandler;
import com.evolution.dropfilecli.client.DaemonClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "rm",
        description = "Remove file"
)
public class ShareRmCommand implements CommandHttpHandler<Void> {

    @CommandLine.ArgGroup(multiplicity = "1")
    private Exclusive exclusive;

    private static class Exclusive {
        @CommandLine.Option(names = {"-id", "--id"}, description = "Id")
        private String id;

        @CommandLine.Option(names = {"-all", "--all"}, description = "Remove all files")
        private boolean all;
    }

    private final DaemonClient daemonClient;

    private final ObjectMapper objectMapper;

    @Autowired
    public ShareRmCommand(DaemonClient daemonClient,
                          ObjectMapper objectMapper) {
        this.daemonClient = daemonClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public HttpResponse<Void> execute() throws Exception {
        if (exclusive.id != null) {
            return daemonClient.shareRm(exclusive.id);
        } else if (exclusive.all) {
            return daemonClient.shareRmAll();
        }
        throw new RuntimeException();
    }

    @Override
    public void handleSuccessful(HttpResponse<Void> response) throws Exception {
        if (exclusive.id != null) {
            System.out.println("Removed: " + exclusive.id);
        } else {
            System.out.println("Removed all files");
        }
    }
}

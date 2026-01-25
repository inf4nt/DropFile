package com.evolution.dropfilecli.command.download;

import com.evolution.dropfilecli.CommandHttpHandler;
import com.evolution.dropfilecli.client.DaemonClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "rm"
)
@RequiredArgsConstructor
public class DownloadRmCommand implements CommandHttpHandler<byte[]> {

    @CommandLine.ArgGroup(multiplicity = "1")
    private Exclusive exclusive;

    private static class Exclusive {
        @CommandLine.Option(names = {"-id", "--id"}, description = "Operation id")
        private String id;

        @CommandLine.Option(names = {"-all", "--all"}, description = "Remove all")
        private boolean all;
    }

    private final DaemonClient daemonClient;

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        if (exclusive.id != null) {
            return daemonClient.downloadRm(exclusive.id);
        } else if (exclusive.all) {
            return daemonClient.downloadRmAll();
        }
        throw new RuntimeException();
    }

    @Override
    public void handleSuccessful(HttpResponse<byte[]> response) throws Exception {
        if (exclusive.id != null) {
            System.out.println("Removed: " + exclusive.id);
        } else {
            System.out.println("Removed all");
        }
    }
}
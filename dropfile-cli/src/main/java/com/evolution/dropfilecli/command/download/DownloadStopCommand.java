package com.evolution.dropfilecli.command.download;

import com.evolution.dropfilecli.CommandHttpHandler;
import com.evolution.dropfilecli.client.DaemonClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "stop"
)
@RequiredArgsConstructor
public class DownloadStopCommand implements CommandHttpHandler<byte[]> {

    @CommandLine.ArgGroup(multiplicity = "1")
    private Exclusive exclusive;

    private static class Exclusive {
        @CommandLine.Option(names = {"-id", "--id"}, description = "operation id")
        private String id;

        @CommandLine.Option(names = {"-all", "--all"}, description = "rm all")
        private boolean all;
    }

    private final DaemonClient daemonClient;

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        if (exclusive.id != null) {
            return daemonClient.downloadStop(exclusive.id);
        } else if (exclusive.all) {
            return daemonClient.downloadStopAll();
        }
        throw new RuntimeException();
    }

    @Override
    public void handleSuccessful(HttpResponse<byte[]> response) throws Exception {
        if (exclusive.id != null) {
            System.out.println("Stopped: " + exclusive.id);
        } else {
            System.out.println("Stopped all");
        }
    }
}
package com.evolution.dropfilecli.command.downloads;

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
public class DownloadsStopCommand implements CommandHttpHandler<byte[]> {

    @CommandLine.Parameters(index = "0", description = "Operation id")
    private String operation;

    private final DaemonClient daemonClient;

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.downloadsStop(operation);
    }

    @Override
    public void handleSuccessful(HttpResponse<byte[]> response) throws Exception {
        System.out.println("Operation successful");
    }
}
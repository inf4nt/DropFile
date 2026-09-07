package com.evolution.dropfilecli.command.connections.download;

import com.evolution.dropfilecli.command.AbstractCommandHttpHandler;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "rm-all",
        description = "Remove all download process",
        customSynopsis = {
                "dropf connections download rm-all"
        }
)
public class DownloadRmAllCommand extends AbstractCommandHttpHandler<Void> {

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.connectionsDownloadRmAll();
    }
}
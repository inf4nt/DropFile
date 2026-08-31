package com.evolution.dropfilecli.command.connections.download;

import com.evolution.dropfilecli.command.AbstractCommandHttpHandler;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "stop-all",
        description = "Stop all download processes",
        customSynopsis = {
                "dropfile connections download stop-all"
        }
)
public class DownloadStopAllCommand extends AbstractCommandHttpHandler<Void> {

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.downloadStopAll();
    }
}
package com.evolution.dropfilecli.command.connections.download;

import com.evolution.dropfilecli.command.AbstractCommandHttpHandler;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "kill-all",
        description = "Kill(stop and remove) all download processes",
        customSynopsis = {
                "dropf connections download kill-all"
        },
        parameterListHeading = "%nRequired parameters:%n",
        optionListHeading = "%nOptional parameters:%n"
)
public class DownloadKillAllCommand extends AbstractCommandHttpHandler<Void> {

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.connectionsDownloadKillAll();
    }
}

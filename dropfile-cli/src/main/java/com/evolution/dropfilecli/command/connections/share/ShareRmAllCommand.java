package com.evolution.dropfilecli.command.connections.share;

import com.evolution.dropfilecli.command.AbstractCommandHttpHandler;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "rm-all",
        description = "Remove all shared files",
        customSynopsis = {
                "dropfile connections share rm-all"
        }
)
public class ShareRmAllCommand extends AbstractCommandHttpHandler<Void> {

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.connectionsShareRmAll();
    }
}

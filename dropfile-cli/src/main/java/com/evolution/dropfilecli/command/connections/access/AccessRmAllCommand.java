package com.evolution.dropfilecli.command.connections.access;

import com.evolution.dropfilecli.command.AbstractCommandHttpHandler;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "rm-all",
        description = "Remove all access keys",
        customSynopsis = {
                "dropfile connections access rm-all"
        }
)
public class AccessRmAllCommand extends AbstractCommandHttpHandler<Void> {

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.connectionsAccessRmAll();
    }
}

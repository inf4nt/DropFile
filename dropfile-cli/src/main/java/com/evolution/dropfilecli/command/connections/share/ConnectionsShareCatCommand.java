package com.evolution.dropfilecli.command.connections.share;

import com.evolution.dropfilecli.AbstractCommandHttpHandler;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "cat",
        description = "Cat file"
)
public class ConnectionsShareCatCommand extends AbstractCommandHttpHandler {

    @CommandLine.Parameters(index = "0", description = "File id")
    private String id;

    @Override
    public HttpResponse<byte[]> execute() {
        return daemonClient.connectionsShareCat(id);
    }

    @Override
    protected void handleSuccessful(HttpResponse<byte[]> response) throws Exception {
        String responseBody = new String(response.body());
        System.out.println(responseBody);
    }
}

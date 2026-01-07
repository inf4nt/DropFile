package com.evolution.dropfilecli.command.connections.share;

import com.evolution.dropfilecli.CommandHttpHandler;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "cat",
        description = "Cat file"
)
public class ConnectionsShareCatCommand implements CommandHttpHandler<byte[]> {
    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return null;
    }

    @Override
    public void handleSuccessful(HttpResponse<byte[]> response) throws Exception {

    }
}

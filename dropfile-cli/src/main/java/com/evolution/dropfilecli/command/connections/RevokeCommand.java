package com.evolution.dropfilecli.command.connections;

import com.evolution.dropfilecli.CommandHttpHandler;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "revoke",
        description = "Drop trusted-in connection"
)
public class RevokeCommand implements CommandHttpHandler<Void> {
    @Override
    public HttpResponse<Void> execute() throws Exception {
        return null;
    }

    @Override
    public void handleSuccessful(HttpResponse<Void> response) throws Exception {

    }
}

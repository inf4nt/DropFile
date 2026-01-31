package com.evolution.dropfilecli.command.link;

import com.evolution.dropfilecli.CommandHttpHandler;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "ls"
)
public class LinkLsCommand implements CommandHttpHandler<byte[]> {
    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return null;
    }

    @Override
    public void handleSuccessful(HttpResponse<byte[]> response) throws Exception {

    }
}

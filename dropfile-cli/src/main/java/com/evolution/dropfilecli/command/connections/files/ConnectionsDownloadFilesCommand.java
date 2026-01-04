package com.evolution.dropfilecli.command.connections.files;

import com.evolution.dropfilecli.CommandHttpHandler;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "download",
        description = "Download file"
)
public class ConnectionsDownloadFilesCommand implements CommandHttpHandler<byte[]> {

    @CommandLine.Parameters(index = "0", description = "File id")
    private String id;

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return null;
    }

    @Override
    public void handleSuccessful(HttpResponse<byte[]> response) throws Exception {

    }
}

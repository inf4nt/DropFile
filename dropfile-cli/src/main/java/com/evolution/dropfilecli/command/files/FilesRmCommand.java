package com.evolution.dropfilecli.command.files;

import com.evolution.dropfilecli.CommandHttpHandler;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "rm",
        description = "Remove file"
)
public class FilesRmCommand implements CommandHttpHandler<Void> {
    @Override
    public HttpResponse<Void> execute() throws Exception {
        return null;
    }

    @Override
    public void handleSuccessful(HttpResponse<Void> response) throws Exception {

    }
}

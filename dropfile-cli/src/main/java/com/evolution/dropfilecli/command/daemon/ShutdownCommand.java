package com.evolution.dropfilecli.command.daemon;

import com.evolution.dropfilecli.AbstractCommandHttpHandler;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "shutdown",
        description = "Daemon shutdown"
)
public class ShutdownCommand extends AbstractCommandHttpHandler {

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.daemonShutdown();
    }
}

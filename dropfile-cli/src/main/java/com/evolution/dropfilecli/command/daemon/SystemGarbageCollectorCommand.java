package com.evolution.dropfilecli.command.daemon;

import com.evolution.dropfilecli.command.AbstractCommandHttpHandler;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "gc",
        aliases = {"purge", "clean"},
        description = "Triggers internal garbage collection and cleanup of stale state",
        mixinStandardHelpOptions = true
)
public class SystemGarbageCollectorCommand extends AbstractCommandHttpHandler<Void> {

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.daemonGarbageCollector();
    }
}

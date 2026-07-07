package com.evolution.dropfilecli.command.daemon;

import com.evolution.dropfilecli.command.AbstractCommandHttpHandler;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "cache-reset",
        description = "Daemon cache reset"
)
public class CacheResetCommand extends AbstractCommandHttpHandler<Void> {

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.daemonCacheReset();
    }
}

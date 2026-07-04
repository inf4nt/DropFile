package com.evolution.dropfilecli.command.quickshare;

import com.evolution.dropfilecli.AbstractCommandHttpHandler;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "rm"
)
public class QuickShareRmCommand extends AbstractCommandHttpHandler<Void> {

    @CommandLine.ArgGroup(multiplicity = "1")
    private Exclusive exclusive;

    private static class Exclusive {
        @CommandLine.Option(names = {"-id", "--id"})
        private String id;

        @CommandLine.Option(names = {"-all", "--all"})
        private boolean all;
    }

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        if (exclusive.all) {
            return daemonClient.quickShareRmAll();
        }
        return daemonClient.quickShareRm(exclusive.id);
    }
}

package com.evolution.dropfilecli.command.link;

import com.evolution.dropfilecli.AbstractCommandHttpHandler;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "rm"
)
public class LinkShareRmCommand extends AbstractCommandHttpHandler {

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
            return daemonClient.linkShareRmAll();
        }
        return daemonClient.linkShareRm(exclusive.id);
    }
}

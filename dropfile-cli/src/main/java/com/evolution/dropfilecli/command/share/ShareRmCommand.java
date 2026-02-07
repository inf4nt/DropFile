package com.evolution.dropfilecli.command.share;

import com.evolution.dropfilecli.AbstractCommandHttpHandler;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "rm",
        description = "Remove file"
)
public class ShareRmCommand extends AbstractCommandHttpHandler {

    @CommandLine.ArgGroup(multiplicity = "1")
    private Exclusive exclusive;

    private static class Exclusive {
        @CommandLine.Option(names = {"-id", "--id"}, description = "Remove by id")
        private String id;

        @CommandLine.Option(names = {"-all", "--all"}, description = "Remove all")
        private boolean all;
    }

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        if (exclusive.all) {
            return daemonClient.shareRmAll();
        }
        return daemonClient.shareRm(exclusive.id);
    }
}

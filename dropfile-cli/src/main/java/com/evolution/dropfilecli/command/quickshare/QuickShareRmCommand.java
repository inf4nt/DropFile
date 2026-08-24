package com.evolution.dropfilecli.command.quickshare;

import com.evolution.dropfilecli.command.AbstractCommandHttpHandler;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "rm",
        description = "Remove quickshare file",
        customSynopsis = {
                "dropfile quickshare rm <id>",
                "dropfile quickshare rm --all"
        },
        parameterListHeading = "%nRequired parameters:%n",
        optionListHeading = "%nOptional parameters:%n",
        sortOptions = false
)
public class QuickShareRmCommand extends AbstractCommandHttpHandler<Void> {

    @CommandLine.ArgGroup(multiplicity = "1")
    private Exclusive exclusive;

    private static class Exclusive {
        @CommandLine.Parameters(index = "0", description = "Quickshare file id")
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

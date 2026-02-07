package com.evolution.dropfilecli.command.share;

import com.evolution.dropfilecli.CommandHttpHandler;
import com.evolution.dropfilecli.client.DaemonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "rm",
        description = "Remove file"
)
public class ShareRmCommand implements CommandHttpHandler<byte[]> {

    @CommandLine.ArgGroup(multiplicity = "1")
    private Exclusive exclusive;

    private static class Exclusive {
        @CommandLine.Option(names = {"-id", "--id"}, description = "Remove by id")
        private String id;

        @CommandLine.Option(names = {"-all", "--all"}, description = "Remove all")
        private boolean all;
    }

    private final DaemonClient daemonClient;

    @Autowired
    public ShareRmCommand(DaemonClient daemonClient) {
        this.daemonClient = daemonClient;
    }

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        if (!ObjectUtils.isEmpty(exclusive.id)) {
            return daemonClient.shareRm(exclusive.id);
        } else if (exclusive.all) {
            return daemonClient.shareRmAll();
        }
        throw new IllegalArgumentException("Command cannot be executed. Check its variables");
    }

    @Override
    public void handleSuccessful(HttpResponse<byte[]> response) throws Exception {
        System.out.println("Completed");
    }
}

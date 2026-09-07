package com.evolution.dropfilecli.command.quickshare;

import com.evolution.dropfilecli.command.AbstractCommandHttpHandler;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "rm-all",
        description = "Remove All quickshare files",
        customSynopsis = {
                "dropf quickshare rm-all"
        }
)
public class QuickShareRmAllCommand extends AbstractCommandHttpHandler<Void> {

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.quickShareRmAll();
    }
}

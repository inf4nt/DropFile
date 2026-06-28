package com.evolution.dropfilecli.command.quickshare;

import com.evolution.dropfile.common.dto.ApiQuickShareLsResponseDTO;
import com.evolution.dropfilecli.AbstractCommandHttpHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.io.File;
import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "add"
)
public class QuickShareAddCommand extends AbstractCommandHttpHandler {

    @CommandLine.Option(names = {"-file", "--file", "-f", "--f"}, description = "File path", required = true)
    private File file;

    @CommandLine.Option(names = {"-alias", "--alias"}, description = "Alias")
    private String alias;

    @CommandLine.Option(names = {"-singleUse", "--singleUse"}, description = "Single use")
    private boolean singleUse;

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.quickShareAdd(file, alias, singleUse);
    }

    @Override
    protected TypeReference<?> getTypeReference() {
        return new TypeReference<ApiQuickShareLsResponseDTO>() {
        };
    }
}

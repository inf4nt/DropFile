package com.evolution.dropfilecli.command.connections.share;

import com.evolution.dropfile.common.dto.ApiConnectionsShareLsResponseDTO;
import com.evolution.dropfile.common.dto.ApiShareInfoResponseDTO;
import com.evolution.dropfilecli.AbstractCommandHttpHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.io.File;
import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "add",
        description = "Add file command"
)
public class ConnectionsShareAddCommand extends AbstractCommandHttpHandler {

    @CommandLine.Option(names = {"-file", "--file", "-f", "--f"}, description = "File path", required = true)
    private File file;

    @CommandLine.Option(names = {"-alias", "--alias"}, description = "Alias")
    private String alias;

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        String filename = getFilename();
        return daemonClient.connectionsShareAdd(filename, file.getAbsolutePath());
    }

    @Override
    protected TypeReference<?> getTypeReference() {
        return new TypeReference<ApiConnectionsShareLsResponseDTO>() {
        };
    }

    private String getFilename() {
        if (alias == null) {
            return file.getName();
        }
        return alias;
    }
}

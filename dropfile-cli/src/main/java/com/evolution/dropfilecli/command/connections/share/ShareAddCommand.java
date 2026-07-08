package com.evolution.dropfilecli.command.connections.share;

import com.evolution.dropfile.common.dto.ApiConnectionsShareLsResponseDTO;
import com.evolution.dropfilecli.command.AbstractCommandHttpHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import picocli.CommandLine;

import java.io.File;
import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "add",
        description = "Add file command"
)
public class ShareAddCommand extends AbstractCommandHttpHandler<ApiConnectionsShareLsResponseDTO> {

    @CommandLine.Option(names = {"-file", "--file", "-f", "--f"}, description = "File path", required = true)
    private File file;

    @CommandLine.Option(names = {"-alias", "--alias"}, description = "Alias")
    private String alias;

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        String alias = getAlias();
        return daemonClient.connectionsShareAdd(file.toPath().toAbsolutePath().normalize().toString(), alias);
    }

    @Override
    protected TypeReference<ApiConnectionsShareLsResponseDTO> getTypeReference() {
        return new TypeReference<ApiConnectionsShareLsResponseDTO>() {
        };
    }

    private String getAlias() {
        if (ObjectUtils.isEmpty(alias)) {
            return file.getName();
        }
        return alias;
    }
}

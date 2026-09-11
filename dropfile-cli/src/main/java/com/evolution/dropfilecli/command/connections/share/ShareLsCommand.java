package com.evolution.dropfilecli.command.connections.share;

import com.evolution.dropfile.common.dto.ApiConnectionsShareLsResponseDTO;
import com.evolution.dropfilecli.command.AbstractCommandHttpHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.stream.Collectors;

@Component
@CommandLine.Command(
        name = "ls",
        description = "Retrieve shared files",
        customSynopsis = "dropf connections share ls [options]",
        parameterListHeading = "%nRequired parameters:%n",
        optionListHeading = "%nOptional parameters:%n"
)
public class ShareLsCommand extends AbstractCommandHttpHandler<List<ApiConnectionsShareLsResponseDTO>> {

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.connectionsShareLs();
    }

    @Override
    protected TypeReference<List<ApiConnectionsShareLsResponseDTO>> getTypeReference() {
        return new TypeReference<List<ApiConnectionsShareLsResponseDTO>>() {
        };
    }

    @Override
    protected void print(List<ApiConnectionsShareLsResponseDTO> object) {
        super.print(object);
        String ids = object.stream()
                .filter(it -> !it.accessible())
                .map(it -> it.id())
                .collect(Collectors.joining(", "));
        if (!ids.isEmpty()) {
            String message = "Inaccessible resources detected. They do not exist or have been modified since they were added: %s".formatted(ids);
            System.out.println(message);
        }
    }
}

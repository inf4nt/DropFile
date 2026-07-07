package com.evolution.dropfilecli.command.connections.browse;

import com.evolution.dropfile.common.dto.ApiConnectionsBrowseGetResponseDTO;
import com.evolution.dropfilecli.command.AbstractCommandHttpHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "get",
        aliases = {"-g", "--g"},
        description = "Get file"
)
public class BrowseGetCommand extends AbstractCommandHttpHandler<ApiConnectionsBrowseGetResponseDTO> {

    @CommandLine.Option(names = {"-id", "--id"}, required = true)
    private String id;

    @CommandLine.Option(names = {"-filename", "--filename", "-f", "--f"})
    private String filename;

    @Override
    public HttpResponse<byte[]> execute() {
        return daemonClient.connectionsBrowseGet(id, filename);
    }

    @Override
    protected TypeReference<ApiConnectionsBrowseGetResponseDTO> getTypeReference() {
        return new TypeReference<ApiConnectionsBrowseGetResponseDTO>() {
        };
    }
}

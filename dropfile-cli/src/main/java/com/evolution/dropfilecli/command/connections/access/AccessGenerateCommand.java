package com.evolution.dropfilecli.command.connections.access;

import com.evolution.dropfile.common.dto.ApiConnectionsAccessInfoResponseDTO;
import com.evolution.dropfilecli.command.AbstractCommandHttpHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "generate",
        aliases = {"g"},
        description = "Generate access key",
        customSynopsis = "dropf connections access generate"
)
public class AccessGenerateCommand extends AbstractCommandHttpHandler<ApiConnectionsAccessInfoResponseDTO> {

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.connectionsAccessGenerate(false);
    }

    @Override
    protected TypeReference<ApiConnectionsAccessInfoResponseDTO> getTypeReference() {
        return new TypeReference<ApiConnectionsAccessInfoResponseDTO>() {
        };
    }

    @Override
    protected void print(ApiConnectionsAccessInfoResponseDTO object) throws Exception {
        spec.commandLine()
                .getParent()
                .getSubcommands()
                .get("show")
                .execute(
                        object.id()
                );
    }
}

package com.evolution.dropfilecli.command.connections.browse;

import com.evolution.dropfile.common.CriteriaEnvelope;
import com.evolution.dropfile.common.dto.ApiConnectionsBrowseGetResponseDTO;
import com.evolution.dropfilecli.command.AbstractCommandHttpHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "get",
        aliases = {"g"},
        description = "Get file",
        customSynopsis = "dropf connections browse get <id> [options]",
        parameterListHeading = "%nRequired parameters:%n",
        optionListHeading = "%nOptional parameters:%n"
)
public class BrowseGetCommand extends AbstractCommandHttpHandler<ApiConnectionsBrowseGetResponseDTO> {

    @CommandLine.Parameters(index = "0", description = "File id")
    private String idCriteria;

    @CommandLine.Option(names = {"--filename", "-f"})
    private String filename;

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.connectionsBrowseGet(new CriteriaEnvelope(idCriteria), filename);
    }

    @Override
    protected TypeReference<ApiConnectionsBrowseGetResponseDTO> getTypeReference() {
        return new TypeReference<ApiConnectionsBrowseGetResponseDTO>() {
        };
    }
}

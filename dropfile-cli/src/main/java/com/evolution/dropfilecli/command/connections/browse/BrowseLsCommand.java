package com.evolution.dropfilecli.command.connections.browse;

import com.evolution.dropfile.common.CriteriaEnvelope;
import com.evolution.dropfile.common.dto.ApiConnectionsBrowseLsResponseDTO;
import com.evolution.dropfilecli.command.AbstractCommandHttpHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Component
@CommandLine.Command(
        name = "ls",
        description = "Retrieve remote files",
        customSynopsis = "dropf connections browse ls [options]",
        parameterListHeading = "%nRequired parameters:%n",
        optionListHeading = "%nOptional parameters:%n"
)
public class BrowseLsCommand extends AbstractCommandHttpHandler<List<ApiConnectionsBrowseLsResponseDTO>> {

    // TODO add show command to show object by id
    @CommandLine.Parameters(index = "0..*", arity = "0..*", split = ",", description = "Browse ids")
    private Set<String> browseIdCriteria;

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        Collection<CriteriaEnvelope> criteriaEnvelopes = CriteriaEnvelope.map(browseIdCriteria);
        return daemonClient.connectionsBrowseLs(criteriaEnvelopes);
    }

    @Override
    protected TypeReference<List<ApiConnectionsBrowseLsResponseDTO>> getTypeReference() {
        return new TypeReference<List<ApiConnectionsBrowseLsResponseDTO>>() {
        };
    }
}

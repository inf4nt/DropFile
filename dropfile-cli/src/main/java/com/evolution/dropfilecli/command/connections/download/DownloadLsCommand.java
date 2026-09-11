package com.evolution.dropfilecli.command.connections.download;

import com.evolution.dropfile.common.dto.ApiDownloadLsDTO;
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
        description = "Retrieve download processes",
        customSynopsis = "dropf connections download ls [options]",
        parameterListHeading = "%nRequired parameters:%n",
        optionListHeading = "%nOptional parameters:%n"
)
public class DownloadLsCommand extends AbstractCommandHttpHandler<List<ApiDownloadLsDTO.Response>> {

    @CommandLine.Option(
            names = {"--status", "-s"},
            description = "Filter by status: ${COMPLETION-CANDIDATES}",
            converter = StatusEnumConverter.class
    )
    private ApiDownloadLsDTO.Status status;

    @CommandLine.Option(names = {"-limit", "--limit"}, description = "Limit", defaultValue = "0")
    private int limit;

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        if (limit < 0) {
            throw new IllegalArgumentException("Limit cannot be negative");
        }
        int limit = this.limit == 0 ? Integer.MAX_VALUE : this.limit;
        return daemonClient.connectionsDownloadLs(status, limit);
    }

    @Override
    protected TypeReference<List<ApiDownloadLsDTO.Response>> getTypeReference() {
        return new TypeReference<List<ApiDownloadLsDTO.Response>>() {
        };
    }

    private static class StatusEnumConverter implements CommandLine.ITypeConverter<ApiDownloadLsDTO.Status> {
        @Override
        public ApiDownloadLsDTO.Status convert(String value) {
            return ApiDownloadLsDTO.Status.valueOf(value.toUpperCase());
        }
    }

    @Override
    protected void print(List<ApiDownloadLsDTO.Response> object) {
        super.print(object);
        String ids = object.stream()
                .filter(it -> !it.accessible())
                .map(it -> it.operation())
                .collect(Collectors.joining(", "));
        if (!ids.isEmpty()) {
            String message = "Inaccessible resources detected. They do not exist or have been modified since they were added: %s".formatted(ids);
            System.out.println(message);
        }
    }
}
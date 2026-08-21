package com.evolution.dropfilecli.command.connections.download;

import com.evolution.dropfile.common.dto.ApiDownloadLsDTO;
import com.evolution.dropfilecli.command.AbstractCommandHttpHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;
import java.util.List;

@Component
@CommandLine.Command(
        name = "ls",
        description = "List downloads"
)
public class DownloadLsCommand extends AbstractCommandHttpHandler<List<ApiDownloadLsDTO.Response>> {

    @CommandLine.Option(
            names = {"-s", "--status"},
            description = "Filter by status: ${COMPLETION-CANDIDATES}",
            converter = StatusEnumConverter.class
    )
    private ApiDownloadLsDTO.Status status;

    @CommandLine.Option(names = {"-limit", "--limit"}, description = "Limit", defaultValue = "0")
    private Integer limit;

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        int limit = this.limit <= 0 ? Integer.MAX_VALUE : this.limit;
        return daemonClient.downloadLs(status, limit);
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
}
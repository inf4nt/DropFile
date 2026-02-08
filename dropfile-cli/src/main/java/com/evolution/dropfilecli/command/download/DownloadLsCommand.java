package com.evolution.dropfilecli.command.download;

import com.evolution.dropfile.common.dto.ApiDownloadLsDTO;
import com.evolution.dropfilecli.AbstractCommandHttpHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;
import java.util.List;

@Component
@CommandLine.Command(
        name = "ls"
)
public class DownloadLsCommand extends AbstractCommandHttpHandler {

    @CommandLine.ArgGroup(multiplicity = "0..1")
    private Exclusive status;

    @CommandLine.Option(names = {"-limit", "--limit"}, description = "Limit", defaultValue = "0")
    private Integer limit;

    private static class Exclusive {
        @CommandLine.Option(names = {"-downloading", "--downloading", "-d", "--d"}, description = "Find by downloading")
        private boolean downloading;

        @CommandLine.Option(names = {"-completed", "--completed", "-c", "--c"}, description = "Find by completed")
        private boolean completed;

        @CommandLine.Option(names = {"-stopped", "--stopped", "-s", "--s"}, description = "Find by stopped")
        private boolean stopped;

        @CommandLine.Option(names = {"-error", "--error", "-e", "--e"}, description = "Find by error")
        private boolean error;
    }

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        int limit = this.limit <= 0 ? Integer.MAX_VALUE : this.limit;
        return daemonClient.downloadLs(getStatus(), limit);
    }

    private ApiDownloadLsDTO.Status getStatus() {
        if (status == null) {
            return null;
        }

        if (status.downloading) {
            return ApiDownloadLsDTO.Status.DOWNLOADING;
        }
        if (status.completed) {
            return ApiDownloadLsDTO.Status.COMPLETED;
        }
        if (status.stopped) {
            return ApiDownloadLsDTO.Status.STOPPED;
        }
        return ApiDownloadLsDTO.Status.ERROR;
    }

    @Override
    protected TypeReference<?> getTypeReference() {
        return new TypeReference<List<ApiDownloadLsDTO.Response>>() {
        };
    }
}
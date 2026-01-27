package com.evolution.dropfilecli.command.download;

import com.evolution.dropfile.common.PrintReflection;
import com.evolution.dropfile.common.dto.ApiDownloadLsDTO;
import com.evolution.dropfilecli.CommandHttpHandler;
import com.evolution.dropfilecli.client.DaemonClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;
import java.util.List;

@Component
@CommandLine.Command(
        name = "ls"
)
@RequiredArgsConstructor
public class DownloadLsCommand implements CommandHttpHandler<byte[]> {

    private final DaemonClient daemonClient;

    private final ObjectMapper objectMapper;

    @CommandLine.ArgGroup(multiplicity = "0..1")
    private Exclusive status;

    @CommandLine.Option(names = {"-limit", "--limit"}, description = "Limit", defaultValue = "0")
    private Integer limit;

    private static class Exclusive {
        @CommandLine.Option(names = {"-downloading", "--downloading"}, description = "Find by downloading")
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
    public void handleSuccessful(HttpResponse<byte[]> response) throws Exception {
        List<ApiDownloadLsDTO.Response> values = objectMapper.readValue(response.body(), new TypeReference<List<ApiDownloadLsDTO.Response>>() {
        });
        if (!values.isEmpty()) {
            PrintReflection.print(values);
        } else {
            System.out.println("No found");
        }
    }
}
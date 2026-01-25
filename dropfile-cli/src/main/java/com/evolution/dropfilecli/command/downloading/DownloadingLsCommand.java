package com.evolution.dropfilecli.command.downloading;

import com.evolution.dropfile.common.PrintReflection;
import com.evolution.dropfile.common.dto.ApiDownloadFileResponse;
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
public class DownloadingLsCommand implements CommandHttpHandler<byte[]> {

    private final DaemonClient daemonClient;

    private final ObjectMapper objectMapper;

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.downloadingLs();
    }

    @Override
    public void handleSuccessful(HttpResponse<byte[]> response) throws Exception {
        List<ApiDownloadFileResponse> values = objectMapper.readValue(response.body(), new TypeReference<List<ApiDownloadFileResponse>>() {
        });
        if (!values.isEmpty()) {
            PrintReflection.print(values);
        } else {
            System.out.println("No downloading found");
        }
    }
}
package com.evolution.dropfilecli.command.share;

import com.evolution.dropfile.common.PrintReflection;
import com.evolution.dropfile.common.dto.ApiShareInfoResponseDTO;
import com.evolution.dropfilecli.CommandHttpHandler;
import com.evolution.dropfilecli.client.DaemonClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.io.File;
import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "add",
        description = "Add file command"
)
public class ShareAddCommand implements CommandHttpHandler<byte[]> {

    @CommandLine.Parameters(index = "0", description = "File path")
    private File file;

    @CommandLine.Option(names = {"-alias", "--alias"}, description = "Alias")
    private String alias;

    private final DaemonClient daemonClient;

    private final ObjectMapper objectMapper;

    @Autowired
    public ShareAddCommand(DaemonClient daemonClient,
                           ObjectMapper objectMapper) {
        this.daemonClient = daemonClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        String filename = getFilename();
        return daemonClient.shareAdd(filename, file.getAbsolutePath());
    }

    @Override
    public void handleSuccessful(HttpResponse<byte[]> response) throws Exception {
        ApiShareInfoResponseDTO responseDTO = objectMapper.readValue(
                response.body(),
                ApiShareInfoResponseDTO.class
        );
        PrintReflection.print(responseDTO);
    }

    private String getFilename() {
        if (alias == null) {
            return file.getName();
        }
        return alias;
    }
}

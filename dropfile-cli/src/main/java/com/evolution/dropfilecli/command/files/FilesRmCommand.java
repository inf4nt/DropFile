package com.evolution.dropfilecli.command.files;

import com.evolution.dropfile.common.PrintReflection;
import com.evolution.dropfile.common.dto.ApiFileInfoResponseDTO;
import com.evolution.dropfilecli.CommandHttpHandler;
import com.evolution.dropfilecli.client.DaemonClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "rm",
        description = "Remove file"
)
public class FilesRmCommand implements CommandHttpHandler<byte[]> {

    @CommandLine.ArgGroup(multiplicity = "1")
    private Exclusive exclusive;

    private static class Exclusive {
        @CommandLine.Option(names = {"-id", "--id"}, description = "Id")
        private String id;

        @CommandLine.Option(names = {"-all", "--all"}, description = "Remove all files")
        private boolean all;
    }

    private final DaemonClient daemonClient;

    private final ObjectMapper objectMapper;

    @Autowired
    public FilesRmCommand(DaemonClient daemonClient,
                          ObjectMapper objectMapper) {
        this.daemonClient = daemonClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        if (exclusive.id != null) {
            return daemonClient.removeFile(exclusive.id);
        } else if (exclusive.all) {
            return daemonClient.removeAllFiles();
        }
        throw new RuntimeException();
    }

    @Override
    public void handleSuccessful(HttpResponse<byte[]> response) throws Exception {
        if (exclusive.id != null) {
            ApiFileInfoResponseDTO responseDTO = objectMapper.readValue(
                    response.body(),
                    ApiFileInfoResponseDTO.class
            );
            PrintReflection.print(responseDTO);
        } else {
            System.out.println("Removed all files");
        }
    }
}

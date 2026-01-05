package com.evolution.dropfilecli.command.files;

import com.evolution.dropfile.common.PrintReflection;
import com.evolution.dropfile.common.dto.ApiFileInfoResponseDTO;
import com.evolution.dropfilecli.CommandHttpHandler;
import com.evolution.dropfilecli.client.DaemonClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.http.HttpResponse;
import java.nio.file.Files;

@Component
@CommandLine.Command(
        name = "add",
        description = "Add file command"
)
public class FilesAddCommand implements CommandHttpHandler<byte[]> {

    @CommandLine.Parameters(index = "0", description = "File path")
    private File file;

    @CommandLine.Option(names = {"-alias", "--alias"}, description = "Alias")
    private String alias;

    private final DaemonClient daemonClient;

    private final ObjectMapper objectMapper;

    @Autowired
    public FilesAddCommand(DaemonClient daemonClient,
                           ObjectMapper objectMapper) {
        this.daemonClient = daemonClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        File toAdd = new File(file.getAbsolutePath());
        if (Files.notExists(toAdd.toPath())) {
            throw new FileNotFoundException(file.getAbsolutePath());
        }
        if (Files.isDirectory(toAdd.toPath())) {
            throw new UnsupportedOperationException(file.getAbsolutePath() + " is a directory");
        }
        String absolutePath = toAdd.getCanonicalFile().getAbsolutePath();
        String filename = getFilename();
        return daemonClient.addFile(filename, absolutePath);
    }

    @Override
    public void handleSuccessful(HttpResponse<byte[]> response) throws Exception {
        ApiFileInfoResponseDTO responseDTO = objectMapper.readValue(
                response.body(),
                ApiFileInfoResponseDTO.class
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

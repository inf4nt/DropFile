package com.evolution.dropfilecli.command.share;

import com.evolution.dropfile.common.dto.ApiShareInfoResponseDTO;
import com.evolution.dropfilecli.AbstractCommandHttpHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.io.File;
import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "add",
        description = "Add file command"
)
public class ShareAddCommand extends AbstractCommandHttpHandler {

    @CommandLine.Parameters(index = "0", description = "File path")
    private File file;

    @CommandLine.Option(names = {"-alias", "--alias"}, description = "Alias")
    private String alias;

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        String filename = getFilename();
        return daemonClient.shareAdd(filename, file.getAbsolutePath());
    }

    @Override
    protected TypeReference<?> getTypeReference() {
        return new TypeReference<ApiShareInfoResponseDTO>() {
        };
    }

    private String getFilename() {
        if (alias == null) {
            return file.getName();
        }
        return alias;
    }
}

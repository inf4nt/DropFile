package com.evolution.dropfilecli.command.share;

import com.evolution.dropfile.common.dto.ApiShareInfoResponseDTO;
import com.evolution.dropfilecli.AbstractCommandHttpHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;
import java.util.List;

@Component
@CommandLine.Command(
        name = "ls",
        description = "LS command"
)
public class ShareLsCommand extends AbstractCommandHttpHandler {

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.shareLs();
    }

    @Override
    protected TypeReference<?> getTypeReference() {
        return new TypeReference<List<ApiShareInfoResponseDTO>>() {
        };
    }
}

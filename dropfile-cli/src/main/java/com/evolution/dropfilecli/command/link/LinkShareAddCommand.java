package com.evolution.dropfilecli.command.link;

import com.evolution.dropfile.common.dto.ApiLinkShareLsResponseDTO;
import com.evolution.dropfilecli.AbstractCommandHttpHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "add"
)
public class LinkShareAddCommand extends AbstractCommandHttpHandler {

    @CommandLine.Option(names = {"-id", "--id"}, description = "File id")
    private String id;

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.linkShareAdd(id);
    }

    @Override
    protected TypeReference<?> getTypeReference() {
        return new TypeReference<ApiLinkShareLsResponseDTO>() {
        };
    }
}

package com.evolution.dropfilecli.command.link;

import com.evolution.dropfile.common.dto.ApiLinkShareLsResponseDTO;
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
public class LinkShareLsCommand extends AbstractCommandHttpHandler {

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.linkShareLs();
    }

    @Override
    protected TypeReference<?> getTypeReference() {
        return new TypeReference<List<ApiLinkShareLsResponseDTO>>() {};
    }
}

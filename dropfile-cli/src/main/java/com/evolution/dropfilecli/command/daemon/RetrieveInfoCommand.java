package com.evolution.dropfilecli.command.daemon;

import com.evolution.dropfile.common.dto.DaemonInfoResponseDTO;
import com.evolution.dropfilecli.command.AbstractCommandHttpHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "status",
        description = "Daemon status"
)
public class RetrieveInfoCommand extends AbstractCommandHttpHandler<DaemonInfoResponseDTO> {

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.daemonInfo();
    }

    @Override
    protected TypeReference<DaemonInfoResponseDTO> getTypeReference() {
        return new TypeReference<DaemonInfoResponseDTO>() {
        };
    }
}

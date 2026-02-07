package com.evolution.dropfilecli.command.connections;

import com.evolution.dropfile.common.CommonUtils;
import com.evolution.dropfile.common.dto.ApiHandshakeStatusResponseDTO;
import com.evolution.dropfilecli.AbstractCommandHttpHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "connect",
        aliases = {"-c", "--c"},
        description = "Connect"
)
public class ConnectCommand extends AbstractCommandHttpHandler {

    @CommandLine.Parameters(index = "0", description = "<host>:<port>")
    private String address;

    @CommandLine.Parameters(index = "1", description = "Secret connection key", defaultValue = "")
    private String key;

    @Override
    public HttpResponse<byte[]> execute() {
        if (!ObjectUtils.isEmpty(key)) {
            System.out.println("Connecting...");
            return daemonClient.handshake(CommonUtils.toURI(address), key);
        }
        System.out.println("Reconnecting...");
        return daemonClient.handshakeReconnect(CommonUtils.toURI(address));
    }

    @Override
    protected TypeReference<?> getTypeReference() {
        return new TypeReference<ApiHandshakeStatusResponseDTO>() {
        };
    }
}

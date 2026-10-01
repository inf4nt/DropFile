package com.evolution.dropfilecli.command.connections.access;

import com.evolution.dropfile.common.CriteriaEnvelope;
import com.evolution.dropfile.common.dto.ApiConnectionsAccessInfoResponseDTO;
import com.evolution.dropfilecli.command.AbstractCommandHttpHandler;
import com.evolution.dropfilecli.util.ConsoleQrPrinter;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "show",
        description = "Show access key",
        customSynopsis = "dropf connections access show <id>"
)
public class AccessShowCommand extends AbstractCommandHttpHandler<ApiConnectionsAccessInfoResponseDTO> {

    @CommandLine.Parameters(index = "0", description = "Access key id")
    private String accessKeyId;

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.connectionsAccessShow(new CriteriaEnvelope(accessKeyId));
    }

    @Override
    protected TypeReference<ApiConnectionsAccessInfoResponseDTO> getTypeReference() {
        return new TypeReference<ApiConnectionsAccessInfoResponseDTO>() {
        };
    }

    @Override
    protected void print(ApiConnectionsAccessInfoResponseDTO object) throws Exception {
        super.print(object);

        if (object.commands().isEmpty()) {
            System.out.println("Unable to generate 'connection' QRCode");
        } else {
            System.out.println();
        }

        for (String command : object.commands()) {
            ConsoleQrPrinter.printAsQr(command);
        }
    }
}

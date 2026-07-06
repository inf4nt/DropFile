package com.evolution.dropfilecli.command.quickshare;

import com.evolution.dropfile.common.dto.ApiQuickShareLsResponseDTO;
import com.evolution.dropfilecli.AbstractCommandHttpHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.io.File;
import java.net.http.HttpResponse;

@Component
@CommandLine.Command(
        name = "add"
)
public class QuickShareAddCommand extends AbstractCommandHttpHandler<ApiQuickShareLsResponseDTO> {

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @CommandLine.Option(names = {"-file", "--file", "-f", "--f"}, description = "File path", required = true)
    private File file;

    @CommandLine.Option(names = {"-alias", "--alias"}, description = "Alias")
    private String alias;

    @CommandLine.Option(names = {"-secret", "--secret"}, description = "Secret password")
    private String secret;

    @CommandLine.Option(
            names = {"-single-use", "--single-use"},
            arity = "0..1",
            defaultValue = "true",
            fallbackValue = "true",
            description = "Single use"
    )
    private boolean singleUse;

    @CommandLine.Option(
            names = {"-secure", "--secure"},
            arity = "0..1",
            defaultValue = "true",
            fallbackValue = "true",
            description = "Encrypt the file with a password. " +
                    "If 'true' (default), packs it into a password-protected archive. " +
                    "If 'false', shares the file as-is (unprotected)"
    )
    private boolean secure;

    @CommandLine.Option(
            names = {"-qrcode", "--qrcode", "-qr", "--qr"},
            arity = "0..1",
            defaultValue = "true",
            fallbackValue = "true",
            description = "Receive QRCode"
    )
    private boolean qrCode;

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.quickShareAdd(file, alias, singleUse, secure, secret);
    }

    @Override
    protected TypeReference<ApiQuickShareLsResponseDTO> getTypeReference() {
        return new TypeReference<ApiQuickShareLsResponseDTO>() {
        };
    }

    @Override
    protected void print(ApiQuickShareLsResponseDTO object) {
        if (qrCode) {
            spec.commandLine()
                    .getParent()
                    .getSubcommands()
                    .get("show")
                    .execute(
                            "-id", object.id(),
                            "-qrcode"
                    );
        } else {
            super.print(object);
        }
    }
}

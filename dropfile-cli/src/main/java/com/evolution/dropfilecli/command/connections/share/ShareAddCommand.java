package com.evolution.dropfilecli.command.connections.share;

import com.evolution.dropfile.common.dto.ApiConnectionsShareLsResponseDTO;
import com.evolution.dropfilecli.command.AbstractCommandHttpHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.io.File;
import java.net.http.HttpResponse;
import java.util.Scanner;

@Component
@CommandLine.Command(
        name = "add",
        description = "Add file as a shared file",
        customSynopsis = "dropf connections share add <file> [options]",
        parameterListHeading = "%nRequired parameters:%n",
        optionListHeading = "%nOptional parameters:%n"
)
public class ShareAddCommand extends AbstractCommandHttpHandler<ApiConnectionsShareLsResponseDTO> {

    @CommandLine.Parameters(index = "0", description = "File path")
    private File file;

    @CommandLine.Option(names = {"--alias"}, description = "Alias")
    private String alias;

    @CommandLine.Option(names = {"-y"}, description = "Automatic yes to prompts")
    private boolean assumeYes;

    @Override
    public void run() {
        if (assumeYes || runConfirmation()) {
            System.out.println("Confirmed. Executing");
            super.run();
        } else {
            System.out.println("Rejected");
        }
    }

    private boolean runConfirmation() {
        System.out.println("Calculating SHA-256 checksum. Please, DO NOT turn off the screen and wait until it finishes");
        System.out.println("Large files may cause a request timeout if hashing takes too long");
        System.out.print("Enter 'y' to continue... ");

        Scanner scanner = new Scanner(System.in);
        String input = scanner.hasNextLine() ? scanner.nextLine() : "n";

        if (input == null) {
            return false;
        }

        return input.trim().equalsIgnoreCase("y");
    }

    @Override
    public HttpResponse<byte[]> execute() throws Exception {
        return daemonClient.connectionsShareAdd(file.toPath().toAbsolutePath().normalize().toString(), alias);
    }

    @Override
    protected TypeReference<ApiConnectionsShareLsResponseDTO> getTypeReference() {
        return new TypeReference<ApiConnectionsShareLsResponseDTO>() {
        };
    }
}

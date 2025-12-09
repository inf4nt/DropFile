package com.evolution.dropfilecli.command.connections.request;

import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        name = "requests",
        description = "Retrieve connections requests",
        aliases = {"--r", "-r"},
        subcommands = {
                IncomingRequestsCommand.class,
                OutgoingRequestsCommand.class
        }
)
public class RequestsCommand implements Runnable {

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(System.out);
    }
}

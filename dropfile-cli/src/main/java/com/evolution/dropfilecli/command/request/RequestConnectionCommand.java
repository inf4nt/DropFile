package com.evolution.dropfilecli.command.request;

import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        name = "requests",
        description = "Retrieve requests",
        subcommands = {
                IncomingRequestConnectionCommand.class,
                OutgoingRequestConnectionCommand.class,
                ApproveIncomingRequestConnectionCommand.class
        }
)
public class RequestConnectionCommand implements Runnable {

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(System.out);
    }
}

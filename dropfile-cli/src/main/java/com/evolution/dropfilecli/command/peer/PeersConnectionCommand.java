package com.evolution.dropfilecli.command.peer;

import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        name = "peers",
        description = "Peers commands",
        subcommands = {
                PeersConnectionTrustedInCommand.class,
                PeersConnectionTrustedOutCommand.class
        }
)
public class PeersConnectionCommand implements Runnable {

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(System.out);
    }
}

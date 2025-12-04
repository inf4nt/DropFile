package com.evolution.dropfilecli.command.handshake;

import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        name = "handshake",
        description = "Handshake commands",
        subcommands = {
                HandshakeRequestCommand.class
        }
)
public class HandshakeCommand implements Runnable {

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(System.out);
    }
}

package com.evolution.dropfilecli.command.connections.access;

import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        name = "access",
        description = "Access keys command",
        subcommands = {
                AccessGenerateCommand.class,
                AccessLsCommand.class,
                AccessRevokeCommand.class
        }
)
public class AccessCommand implements Runnable {

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(System.out);
    }
}

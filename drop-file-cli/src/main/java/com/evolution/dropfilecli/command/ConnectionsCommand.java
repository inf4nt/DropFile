package com.evolution.dropfilecli.command;

import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        name = "connections",
        description = "Connections commands",
        subcommands = {
                ConnectionsConnectCommand.class,
                ConnectionsOnlineCommand.class
        }
)
public class ConnectionsCommand implements Runnable {

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(System.out);
    }
}

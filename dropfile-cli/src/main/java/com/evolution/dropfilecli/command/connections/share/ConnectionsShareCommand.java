package com.evolution.dropfilecli.command.connections.share;

import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        name = "share",
        description = "Share operations",
        subcommands = {
                ConnectionsShareDownloadCommand.class,
                ConnectionsShareLsCommand.class,
                ConnectionsShareCatCommand.class
        }
)
public class ConnectionsShareCommand implements Runnable {

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(System.out);
    }
}

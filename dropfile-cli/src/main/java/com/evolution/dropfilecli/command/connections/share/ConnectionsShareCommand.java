package com.evolution.dropfilecli.command.connections.share;

import com.evolution.dropfilecli.SimpleCommandHandler;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        name = "share",
        aliases = {"-s", "--s"},
        description = "Share operations",
        subcommands = {
                ConnectionsShareDownloadCommand.class,
                ConnectionsShareLsCommand.class,
                ConnectionsShareCatCommand.class
        }
)
public class ConnectionsShareCommand implements SimpleCommandHandler {

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public void handle() {
        spec.commandLine().usage(System.out);
    }
}

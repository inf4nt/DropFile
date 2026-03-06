package com.evolution.dropfilecli.command.connections;

import com.evolution.dropfilecli.SimpleCommandHandler;
import com.evolution.dropfilecli.command.connections.access.AccessCommand;
import com.evolution.dropfilecli.command.connections.share.ConnectionsShareCommand;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        name = "connections",
        description = "Connections",
        aliases = {"-c", "--c"},
        subcommands = {
                ConnectCommand.class,
                CurrentConnectionCommand.class,
                TrustedInCommand.class,
                TrustedOutCommand.class,
                DisconnectCommand.class,
                RevokeCommand.class,
                AccessCommand.class,
                StatusConnectionCommand.class,
                ConnectionsShareCommand.class
        }
)
public class ConnectionsCommand implements SimpleCommandHandler {

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public void handle() {
        spec.commandLine().usage(System.out);
    }
}

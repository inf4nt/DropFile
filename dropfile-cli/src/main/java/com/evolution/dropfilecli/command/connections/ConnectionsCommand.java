package com.evolution.dropfilecli.command.connections;

import com.evolution.dropfilecli.command.connections.access.AccessCommand;
import com.evolution.dropfilecli.command.connections.files.ConnectionsFilesCommand;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        name = "connections",
        description = "Connections",
        subcommands = {
                ConnectCommand.class,
                CurrentConnectionCommand.class,
                TrustedInCommand.class,
                TrustedOutCommand.class,
                DisconnectCommand.class,
                RevokeCommand.class,
                AccessCommand.class,
                StatusConnectionCommand.class,
                ConnectionsFilesCommand.class
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

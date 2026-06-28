package com.evolution.dropfilecli.command.connections;

import com.evolution.dropfilecli.SimpleCommandHandler;
import com.evolution.dropfilecli.command.connections.access.AccessCommand;
import com.evolution.dropfilecli.command.connections.browse.BrowseCommand;
import com.evolution.dropfilecli.command.connections.download.DownloadCommand;
import com.evolution.dropfilecli.command.connections.share.ShareCommand;
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
                BrowseCommand.class,
                ShareCommand.class,
                DownloadCommand.class,
                TrafficCommand.class
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

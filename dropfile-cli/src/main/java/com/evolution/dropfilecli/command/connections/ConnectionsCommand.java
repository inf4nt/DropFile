package com.evolution.dropfilecli.command.connections;

import com.evolution.dropfilecli.command.AbstractCommandHandler;
import com.evolution.dropfilecli.command.connections.access.AccessCommand;
import com.evolution.dropfilecli.command.connections.browse.BrowseCommand;
import com.evolution.dropfilecli.command.connections.download.DownloadCommand;
import com.evolution.dropfilecli.command.connections.share.ShareCommand;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        name = "connections",
        description = "Connections commands",
        aliases = {"c"},
        subcommands = {
                ConnectCommand.class,
                CurrentConnectionCommand.class,
                TrustedInCommand.class,
                TrustedOutCommand.class,
                DisconnectCommand.class,
                RevokeCommand.class,
                AccessCommand.class,
                ReconnectConnectionCommand.class,
                BrowseCommand.class,
                ShareCommand.class,
                DownloadCommand.class,
                TrafficCommand.class
        }
)
public class ConnectionsCommand extends AbstractCommandHandler {

}

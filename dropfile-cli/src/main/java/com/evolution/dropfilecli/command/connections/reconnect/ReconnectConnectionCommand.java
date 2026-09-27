package com.evolution.dropfilecli.command.connections.reconnect;

import com.evolution.dropfilecli.command.AbstractCommandHandler;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        name = "reconnect",
        aliases = {"r"},
        description = "Reconnect commands",
        subcommands = {
                AliasReconnectConnectionCommand.class,
                FingerprintReconnectConnectionCommand.class,
                AddressReconnectConnectionCommand.class
        }
)
public class ReconnectConnectionCommand extends AbstractCommandHandler {

}

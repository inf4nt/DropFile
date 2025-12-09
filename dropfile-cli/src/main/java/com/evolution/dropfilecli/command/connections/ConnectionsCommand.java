package com.evolution.dropfilecli.command.connections;

import com.evolution.dropfilecli.command.connections.request.RequestsCommand;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        name = "connections",
        description = "Connections",
        subcommands = {
                RequestsCommand.class,
                ConnectCommand.class,
                CurrentConnectionCommand.class,
                TrustedInCommand.class,
                TrustedOutCommand.class,
                ApproveIncomingRequestConnectionCommand.class,
                PingNodeCommand.class,
                DisconnectCommand.class,
                RevokeCommand.class
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

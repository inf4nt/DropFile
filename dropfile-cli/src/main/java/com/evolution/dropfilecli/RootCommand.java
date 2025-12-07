package com.evolution.dropfilecli;

import com.evolution.dropfile.configuration.app.DropFileAppConfig;
import com.evolution.dropfilecli.command.ApproveIncomingRequestConnectionCommand;
import com.evolution.dropfilecli.command.ConnectCommand;
import com.evolution.dropfilecli.command.RetrieveInfoCommand;
import com.evolution.dropfilecli.command.peer.PeersConnectionCommand;
import com.evolution.dropfilecli.command.request.RequestConnectionCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        mixinStandardHelpOptions = true,
        versionProvider = ManifestVersionProvider.class,
        subcommands = {
                ConnectCommand.class,
                RequestConnectionCommand.class,
                PeersConnectionCommand.class,
                ApproveIncomingRequestConnectionCommand.class,
                RetrieveInfoCommand.class
        }
)
public class RootCommand implements Runnable {

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    private final DropFileAppConfig.DropFileCliAppConfig cliAppConfig;

    @Autowired
    public RootCommand(DropFileAppConfig.DropFileCliAppConfig cliAppConfig) {
        this.cliAppConfig = cliAppConfig;
    }

    @Override
    public void run() {
        System.out.println("Daemon host: " + cliAppConfig.getDaemonHost());
        System.out.println("Daemon port: " + cliAppConfig.getDaemonPort());
        spec.commandLine().usage(System.out);
    }
}
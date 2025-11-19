package com.evolution.dropfilecli;

import com.evolution.dropfilecli.command.ConfigurationCommand;
import com.evolution.dropfilecli.command.DownloadCommand;
import com.evolution.dropfilecli.command.FilesCommand;
import com.evolution.dropfilecli.command.NodesCommand;
import com.evolution.dropfilecli.command.connection.ConnectCommand;
import com.evolution.dropfilecli.command.connection.ConnectionStatusCommand;
import com.evolution.dropfilecli.command.connection.DisconnectCommand;
import com.evolution.dropfilecli.command.daemon.DaemonCommand;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        mixinStandardHelpOptions = true,
        versionProvider = ManifestVersionProvider.class,
        subcommands = {
                ConnectCommand.class,
                ConnectionStatusCommand.class,
                DisconnectCommand.class,
                ConfigurationCommand.class,
                FilesCommand.class,
                DownloadCommand.class,
                NodesCommand.class,
                DaemonCommand.class
        }
)
public class RootCommand implements Runnable {

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(System.out);
    }
}
package com.evolution.dropfilecli.old;

import com.evolution.dropfilecli.ManifestVersionProvider;
import com.evolution.dropfilecli.old.command.ConfigurationCommand;
import com.evolution.dropfilecli.old.command.DownloadCommand;
import com.evolution.dropfilecli.old.command.FilesCommand;
import com.evolution.dropfilecli.old.command.NodesCommand;
import com.evolution.dropfilecli.old.command.connection.ConnectCommand;
import com.evolution.dropfilecli.old.command.connection.ConnectionStatusCommand;
import com.evolution.dropfilecli.old.command.connection.DisconnectCommand;
import com.evolution.dropfilecli.old.command.connection.OnlineCommand;
import com.evolution.dropfilecli.old.command.daemon.DaemonCommand;
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
                DaemonCommand.class,
                OnlineCommand.class
        }
)
public class RootCommandOld implements Runnable {

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(System.out);
    }
}
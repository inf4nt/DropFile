package com.evolution.dropfilecli;

import com.evolution.dropfilecli.command.*;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        mixinStandardHelpOptions = true,
        versionProvider = ManifestVersionProvider.class,
        subcommands = {
                ConnectCommand.class,
                StatusCommand.class,
                DisconnectCommand.class,
                ConfigurationCommand.class,
                FilesCommand.class,
                DownloadCommand.class
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
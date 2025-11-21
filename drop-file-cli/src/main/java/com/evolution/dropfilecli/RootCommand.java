package com.evolution.dropfilecli;

import com.evolution.dropfilecli.command.ConnectionsCommand;
import com.evolution.dropfilecli.command.daemon.DaemonCommand;
import com.evolution.dropfilecli.configuration.CliConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        mixinStandardHelpOptions = true,
        versionProvider = ManifestVersionProvider.class,
        subcommands = {
                ConnectionsCommand.class,
                DaemonCommand.class
        }
)
public class RootCommand implements Runnable {

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    private final CliConfig cliConfig;

    @Autowired
    public RootCommand(CliConfig cliConfig) {
        this.cliConfig = cliConfig;
    }

    @Override
    public void run() {
        System.out.println("Daemon address: " + cliConfig.getDaemonAddress());
        System.out.println("Download directory: " + cliConfig.getDownloadDirectory());
        spec.commandLine().usage(System.out);
    }
}
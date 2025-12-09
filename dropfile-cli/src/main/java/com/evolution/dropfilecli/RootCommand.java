package com.evolution.dropfilecli;

import com.evolution.dropfile.configuration.app.DropFileAppConfig;
import com.evolution.dropfilecli.command.connections.ConnectionsCommand;
import com.evolution.dropfilecli.command.daemon.DaemonCommand;
import com.evolution.dropfilecli.command.files.FilesCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        mixinStandardHelpOptions = true,
        versionProvider = ManifestVersionProvider.class,
        subcommands = {
                ConnectionsCommand.class,
                DaemonCommand.class,
                FilesCommand.class
        }
)
public class RootCommand implements Runnable {

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    private final DropFileAppConfig.DropFileCliAppConfig cliAppConfig;

    private final DropFileAppConfig.DropFileDaemonAppConfig daemonAppConfig;

    @Autowired
    public RootCommand(DropFileAppConfig.DropFileCliAppConfig cliAppConfig,
                       DropFileAppConfig.DropFileDaemonAppConfig daemonAppConfig) {
        this.cliAppConfig = cliAppConfig;
        this.daemonAppConfig = daemonAppConfig;
    }

    @Override
    public void run() {
        System.out.println("Daemon host: " + cliAppConfig.getDaemonHost());
        System.out.println("Daemon port: " + cliAppConfig.getDaemonPort());
        System.out.println("Daemon public address URI: " + daemonAppConfig.getPublicDaemonAddressURI());
        spec.commandLine().usage(System.out);
    }
}
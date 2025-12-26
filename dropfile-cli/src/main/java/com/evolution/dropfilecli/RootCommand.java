package com.evolution.dropfilecli;

import com.evolution.dropfile.configuration.app.AppConfig;
import com.evolution.dropfile.configuration.app.AppConfigStore;
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

    private final AppConfigStore appConfigStore;

    @Autowired
    public RootCommand(AppConfigStore appConfigStore) {
        this.appConfigStore = appConfigStore;
    }

    @Override
    public void run() {
        AppConfig appConfig = appConfigStore.getRequired();
        AppConfig.CliAppConfig cliAppConfig = appConfig.cliAppConfig();
        AppConfig.DaemonAppConfig daemonAppConfig = appConfig.daemonAppConfig();

        System.out.println("Daemon host: " + cliAppConfig.daemonHost());
        System.out.println("Daemon port: " + cliAppConfig.daemonPort());
        System.out.println("Daemon public address URI: " + daemonAppConfig.publicDaemonAddressURI());
        spec.commandLine().usage(System.out);
    }
}
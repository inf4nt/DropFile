package com.evolution.dropfilecli;

import com.evolution.dropfile.configuration.app.DropFileAppConfig;
import com.evolution.dropfile.configuration.secret.DropFileSecretsConfig;
import com.evolution.dropfilecli.command.ConnectionsCommand;
import com.evolution.dropfilecli.command.daemon.DaemonCommand;
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

    private final DropFileAppConfig appConfig;

    private final DropFileSecretsConfig secretsConfig;

    @Autowired
    public RootCommand(DropFileAppConfig appConfig,
                       DropFileSecretsConfig secretsConfig) {
        this.appConfig = appConfig;
        this.secretsConfig = secretsConfig;
    }

    @Override
    public void run() {
        System.out.println("Daemon address: " + appConfig.getDaemonAddress());
        System.out.println("Download directory: " + appConfig.getDownloadDirectory());
        if (secretsConfig.getDaemonToken() == null) {
            System.out.println("No daemon token. Daemon is not running.");
        }

        spec.commandLine().usage(System.out);
    }
}
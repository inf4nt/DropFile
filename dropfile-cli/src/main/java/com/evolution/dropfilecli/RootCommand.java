package com.evolution.dropfilecli;

import com.evolution.dropfile.configuration.app.DropFileAppConfig;
import com.evolution.dropfile.configuration.app.DropFileAppConfigManager;
import com.evolution.dropfilecli.command.ConnectionsCommand;
import com.evolution.dropfilecli.command.daemon.DaemonCommand;
import com.evolution.dropfilecli.command.handshake.HandshakeCommand;
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
                HandshakeCommand.class
        }
)
public class RootCommand implements Runnable {

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    private final DropFileAppConfig appConfig;

    @Autowired
    public RootCommand(DropFileAppConfig appConfig) {
        this.appConfig = appConfig;
    }

    @Override
    public void run() {
        System.out.println("Daemon host: " + appConfig.getDaemonHost());
        System.out.println("Daemon port: " + appConfig.getDaemonPort());
        System.out.println("Download directory: " + appConfig.getDownloadDirectory());
        spec.commandLine().usage(System.out);
    }
}
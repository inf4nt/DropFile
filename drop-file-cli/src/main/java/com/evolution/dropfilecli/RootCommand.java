package com.evolution.dropfilecli;

import com.evolution.dropfile.configuration.app.DropFileAppConfig;
import com.evolution.dropfile.configuration.app.DropFileAppConfigManager;
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

    private final DropFileAppConfigManager appConfig;

    @Autowired
    public RootCommand(DropFileAppConfigManager appConfig) {
        this.appConfig = appConfig;
    }

    @Override
    public void run() {
        DropFileAppConfig config = appConfig.get();
        System.out.println("Daemon address: " + config.getDaemonAddress());
        System.out.println("Download directory: " + config.getDownloadDirectory());
        spec.commandLine().usage(System.out);
    }
}
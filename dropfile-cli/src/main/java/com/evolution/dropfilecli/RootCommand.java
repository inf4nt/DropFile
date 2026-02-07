package com.evolution.dropfilecli;

import com.evolution.dropfile.store.app.AppConfig;
import com.evolution.dropfile.store.app.AppConfigStore;
import com.evolution.dropfilecli.command.connections.ConnectionsCommand;
import com.evolution.dropfilecli.command.daemon.DaemonCommand;
import com.evolution.dropfilecli.command.download.DownloadCommand;
import com.evolution.dropfilecli.command.link.LinkCommand;
import com.evolution.dropfilecli.command.share.ShareCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@RequiredArgsConstructor
@Component
@CommandLine.Command(
        mixinStandardHelpOptions = true,
        versionProvider = ManifestVersionProvider.class,
        subcommands = {
                ConnectionsCommand.class,
                DaemonCommand.class,
                ShareCommand.class,
                DownloadCommand.class,
                LinkCommand.class
        }
)
public class RootCommand implements Runnable {

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    private final AppConfigStore appConfigStore;

    @Override
    public void run() {
        AppConfig.CliAppConfig cliAppConfig = appConfigStore.getRequired().cliAppConfig();

        System.out.println("Daemon host: " + cliAppConfig.daemonHost());
        System.out.println("Daemon port: " + cliAppConfig.daemonPort());
        spec.commandLine().usage(System.out);
    }
}
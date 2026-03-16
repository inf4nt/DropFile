package com.evolution.dropfilecli;

import com.evolution.dropfilecli.command.connections.ConnectionsCommand;
import com.evolution.dropfilecli.command.daemon.DaemonCommand;
import com.evolution.dropfilecli.command.download.DownloadCommand;
import com.evolution.dropfilecli.command.link.LinkCommand;
import com.evolution.dropfilecli.command.share.ShareCommand;
import com.evolution.dropfilecli.config.CliApplicationProperties;
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
public class RootCommand implements SimpleCommandHandler {

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    private final CliApplicationProperties cliApplicationProperties;

    @Override
    public void handle() {
        System.out.println("░███████                                      ░████ ░██░██            \n" +
                "░██   ░██                                    ░██       ░██            \n" +
                "░██    ░██ ░██░████  ░███████  ░████████  ░████████ ░██░██  ░███████  \n" +
                "░██    ░██ ░███     ░██    ░██ ░██    ░██    ░██    ░██░██ ░██    ░██ \n" +
                "░██    ░██ ░██      ░██    ░██ ░██    ░██    ░██    ░██░██ ░█████████ \n" +
                "░██   ░██  ░██      ░██    ░██ ░███   ░██    ░██    ░██░██ ░██        \n" +
                "░███████   ░██       ░███████  ░██░█████     ░██    ░██░██  ░███████  \n" +
                "                               ░██                                    \n" +
                "                               ░██                                    \n" +
                "                                                                      ");

        System.out.println("Daemon host: " + cliApplicationProperties.daemonHost);
        System.out.println("Daemon port: " + cliApplicationProperties.daemonPort);
        spec.commandLine().usage(System.out);
    }
}
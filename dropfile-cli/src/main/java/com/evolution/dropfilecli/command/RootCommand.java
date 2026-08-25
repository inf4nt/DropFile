package com.evolution.dropfilecli.command;

import com.evolution.dropfile.common.SystemInfoProvider;
import com.evolution.dropfilecli.command.connections.ConnectionsCommand;
import com.evolution.dropfilecli.command.daemon.DaemonCommand;
import com.evolution.dropfilecli.command.quickshare.QuickShareCommand;
import com.evolution.dropfilecli.config.CliApplicationProperties;
import com.evolution.dropfilecli.util.Spinner;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@RequiredArgsConstructor
@Component
@CommandLine.Command(
        versionProvider = ManifestVersionProvider.class,
        mixinStandardHelpOptions = true,
        subcommands = {
                ConnectionsCommand.class,
                DaemonCommand.class,
                QuickShareCommand.class
        }
)
public class RootCommand implements Runnable {

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    private final CliApplicationProperties cliApplicationProperties;

    private final SystemInfoProvider systemInfoProvider;

    private final ObjectMapper objectMapper;

    @SneakyThrows
    @Override
    public void run() {
        Spinner.stop();

        System.out.println("""
                ░███████                                      ░████ ░██░██           \s
                ░██   ░██                                    ░██       ░██           \s
                ░██    ░██ ░██░████  ░███████  ░████████  ░████████ ░██░██  ░███████ \s
                ░██    ░██ ░███     ░██    ░██ ░██    ░██    ░██    ░██░██ ░██    ░██\s
                ░██    ░██ ░██      ░██    ░██ ░██    ░██    ░██    ░██░██ ░█████████\s
                ░██   ░██  ░██      ░██    ░██ ░███   ░██    ░██    ░██░██ ░██       \s
                ░███████   ░██       ░███████  ░██░█████     ░██    ░██░██  ░███████ \s
                                               ░██                                   \s
                                               ░██                                   \s
                                                                                     \s""");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(systemInfoProvider.getSystemInfo()));
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(cliApplicationProperties));
        spec.commandLine().usage(System.out);
    }
}
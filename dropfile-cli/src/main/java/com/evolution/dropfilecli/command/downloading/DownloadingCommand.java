package com.evolution.dropfilecli.command.downloading;

import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        name = "downloading",
        description = "Downloading command",
        subcommands = {
                DownloadingLsCommand.class
        }
)
public class DownloadingCommand implements Runnable {

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(System.out);
    }
}

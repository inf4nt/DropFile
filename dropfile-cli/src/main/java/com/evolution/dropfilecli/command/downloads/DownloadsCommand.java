package com.evolution.dropfilecli.command.downloads;

import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        name = "downloads",
        description = "Downloads command",
        subcommands = {
                DownloadsLsCommand.class
        }
)
public class DownloadsCommand implements Runnable {

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(System.out);
    }
}

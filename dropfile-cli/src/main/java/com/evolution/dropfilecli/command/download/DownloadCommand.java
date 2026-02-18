package com.evolution.dropfilecli.command.download;

import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        name = "download",
        description = "Download commands",
        aliases = {"-d", "--d"},
        subcommands = {
                DownloadLsCommand.class,
                DownloadStopCommand.class,
                DownloadRmCommand.class
        }
)
public class DownloadCommand implements Runnable {

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(System.out);
    }
}

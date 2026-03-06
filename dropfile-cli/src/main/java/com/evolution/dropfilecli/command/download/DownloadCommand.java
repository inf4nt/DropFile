package com.evolution.dropfilecli.command.download;

import com.evolution.dropfilecli.SimpleCommandHandler;
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
public class DownloadCommand implements SimpleCommandHandler {

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public void handle() {
        spec.commandLine().usage(System.out);
    }
}

package com.evolution.dropfilecli.command.connections.download;

import com.evolution.dropfilecli.command.AbstractCommandHandler;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        name = "download",
        description = "Download commands",
        aliases = {"d"},
        subcommands = {
                DownloadLsCommand.class,
                DownloadKillCommand.class,
                DownloadKillAllCommand.class
        }
)
public class DownloadCommand extends AbstractCommandHandler {

}

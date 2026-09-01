package com.evolution.dropfilecli.command.connections.share;

import com.evolution.dropfilecli.command.AbstractCommandHandler;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        name = "share",
        description = "Share commands",
        aliases = {"s"},
        subcommands = {
                ShareLsCommand.class,
                ShareAddCommand.class,
                ShareRmCommand.class,
                ShareRmAllCommand.class
        }
)
public class ShareCommand extends AbstractCommandHandler {

}

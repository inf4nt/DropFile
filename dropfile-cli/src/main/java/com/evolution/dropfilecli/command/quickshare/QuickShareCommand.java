package com.evolution.dropfilecli.command.quickshare;

import com.evolution.dropfilecli.command.AbstractCommandHandler;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        name = "quickshare",
        description = "Quickshare commands",
        aliases = {"q"},
        subcommands = {
                QuickShareAddCommand.class,
                QuickShareLsCommand.class,
                QuickShareRmCommand.class,
                QuickShareShowCommand.class,
                QuickShareRmAllCommand.class
        }
)
public class QuickShareCommand extends AbstractCommandHandler {
}

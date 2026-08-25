package com.evolution.dropfilecli.command.connections.browse;

import com.evolution.dropfilecli.command.AbstractCommandHandler;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        name = "browse",
        aliases = {"b"},
        description = "Browse operations",
        subcommands = {
                BrowseGetCommand.class,
                BrowseLsCommand.class
        }
)
public class BrowseCommand extends AbstractCommandHandler {

}

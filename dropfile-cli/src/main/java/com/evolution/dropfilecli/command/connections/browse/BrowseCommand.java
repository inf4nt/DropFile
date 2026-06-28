package com.evolution.dropfilecli.command.connections.browse;

import com.evolution.dropfilecli.SimpleCommandHandler;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        name = "browse",
        aliases = {"-b", "--b"},
        description = "Browse operations",
        subcommands = {
                BrowseGetCommand.class,
                BrowseLsCommand.class
        }
)
public class BrowseCommand implements SimpleCommandHandler {

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public void handle() {
        spec.commandLine().usage(System.out);
    }
}

package com.evolution.dropfilecli.command.link;

import com.evolution.dropfilecli.SimpleCommandHandler;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        name = "link",
        description = "Link commands",
        aliases = {"-l", "--l"},
        subcommands = {
                LinkShareAddCommand.class,
                LinkShareLsCommand.class,
                LinkShareRmCommand.class
        }
)
public class LinkShareCommand implements SimpleCommandHandler {
    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public void handle() {
        spec.commandLine().usage(System.out);
    }
}

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
                LinkAddCommand.class,
                LinkLsCommand.class,
                LinkRmCommand.class
        }
)
public class LinkCommand implements SimpleCommandHandler {
    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public void handle() {
        spec.commandLine().usage(System.out);
    }
}

package com.evolution.dropfilecli.command.link;

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
public class LinkCommand implements Runnable {
    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(System.out);
    }
}

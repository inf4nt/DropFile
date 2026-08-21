package com.evolution.dropfilecli.command.quickshare;

import com.evolution.dropfilecli.command.SimpleCommandHandler;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        name = "quickshare",
        description = "Quickshare commands",
        aliases = {"-q", "--q"},
        subcommands = {
                QuickShareAddCommand.class,
                QuickShareLsCommand.class,
                QuickShareRmCommand.class,
                QuickShareShowCommand.class
        }
)
public class QuickShareCommand implements SimpleCommandHandler {
    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public void handle() {
        spec.commandLine().usage(System.out);
    }
}

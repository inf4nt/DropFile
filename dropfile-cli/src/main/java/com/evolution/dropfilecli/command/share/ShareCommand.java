package com.evolution.dropfilecli.command.share;

import com.evolution.dropfilecli.SimpleCommandHandler;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        name = "share",
        description = "Share commands",
        aliases = {"-s", "--s"},
        subcommands = {
                ShareLsCommand.class,
                ShareAddCommand.class,
                ShareRmCommand.class
        }
)
public class ShareCommand implements SimpleCommandHandler {

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public void handle() {
        spec.commandLine().usage(System.out);
    }
}

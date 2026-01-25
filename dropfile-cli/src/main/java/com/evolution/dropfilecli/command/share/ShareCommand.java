package com.evolution.dropfilecli.command.share;

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
public class ShareCommand implements Runnable {

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(System.out);
    }
}

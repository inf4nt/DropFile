package com.evolution.dropfilecli.command.daemon;

import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        name = "daemon",
        description = "Daemon commands",
        subcommands = {
                ShutdownCommand.class,
                StatusCommand.class,
                RetrieveInfoCommand.class
        }
)
public class DaemonCommand implements Runnable {

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(System.out);
    }
}

package com.evolution.dropfilecli.command.daemon;

import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        name = "daemon",
        description = "Daemon commands",
        subcommands = {
                DaemonShutdownCommand.class,
                DaemonStatusCommand.class
        }
)
public class DaemonCommand implements Runnable {

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(System.out);
    }
}

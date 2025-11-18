package com.evolution.dropfilecli.command.daemon;

import picocli.CommandLine;

@CommandLine.Command(
        name = "daemon",
        description = "Daemon command",
        subcommands = {
                DaemonShutdownCommand.class,
                DaemonPingCommand.class
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

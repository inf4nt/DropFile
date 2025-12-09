package com.evolution.dropfilecli.command.daemon.config;

import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        name = "config",
        description = "Config command",
        subcommands = {
                SetPublicAddressCommand.class
        }
)
public class DaemonConfigCommand implements Runnable {

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(System.out);
    }
}
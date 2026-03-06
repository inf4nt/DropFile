package com.evolution.dropfilecli.command.daemon;

import com.evolution.dropfilecli.SimpleCommandHandler;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        name = "daemon",
        description = "Daemon commands",
        subcommands = {
                ShutdownCommand.class,
                RetrieveInfoCommand.class,
                StartCommand.class,
                CacheResetCommand.class
        }
)
public class DaemonCommand implements SimpleCommandHandler {

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public void handle() {
        spec.commandLine().usage(System.out);
    }
}

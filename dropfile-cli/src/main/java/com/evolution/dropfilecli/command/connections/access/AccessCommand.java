package com.evolution.dropfilecli.command.connections.access;

import com.evolution.dropfilecli.SimpleCommandHandler;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        name = "access",
        aliases = {"-a", "--a"},
        description = "Access keys command",
        subcommands = {
                AccessGenerateCommand.class,
                AccessLsCommand.class,
                AccessRmCommand.class
        }
)
public class AccessCommand implements SimpleCommandHandler {

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public void handle() {
        spec.commandLine().usage(System.out);
    }
}

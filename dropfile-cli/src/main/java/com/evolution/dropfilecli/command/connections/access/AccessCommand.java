package com.evolution.dropfilecli.command.connections.access;

import com.evolution.dropfilecli.command.AbstractCommandHandler;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        name = "access",
        aliases = {"a"},
        description = "Access keys command",
        subcommands = {
                AccessGenerateCommand.class,
                AccessLsCommand.class,
                AccessRmCommand.class,
                AccessRmAllCommand.class
        }
)
public class AccessCommand extends AbstractCommandHandler {

}

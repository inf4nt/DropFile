package com.evolution.dropfilecli.command.daemon;

import com.evolution.dropfilecli.command.AbstractCommandHandler;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        name = "daemon",
        aliases = {"d"},
        description = "Daemon commands",
        subcommands = {
                ShutdownCommand.class,
                RetrieveInfoCommand.class,
                StartCommand.class,
                CacheResetCommand.class,
                SystemGarbageCollectorCommand.class
        }
)
public class DaemonCommand extends AbstractCommandHandler {

}

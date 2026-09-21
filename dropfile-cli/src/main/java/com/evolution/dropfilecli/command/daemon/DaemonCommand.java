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
                DaemonShutdownCommand.class,
                DaemonStatusCommand.class,
                DaemonStartCommand.class,
                DaemonCacheResetCommand.class,
                DaemonSystemGarbageCollectorCommand.class
        }
)
public class DaemonCommand extends AbstractCommandHandler {

}

package com.evolution.dropfilecli.command.connections.files;

import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        name = "files",
        description = "File operations",
        subcommands = {
                ConnectionsDownloadFilesCommand.class,
                ConnectionsLsFilesCommand.class
        }
)
public class ConnectionsFilesCommand implements Runnable {

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(System.out);
    }
}

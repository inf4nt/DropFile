package com.evolution.dropfilecli.command.files;

import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        name = "files",
        description = "File operation commands",
        subcommands = {
                FilesLsCommand.class,
                FilesRmCommand.class,
                FilesCatCommand.class
        }
)
public class FilesCommand implements Runnable {

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(System.out);
    }
}


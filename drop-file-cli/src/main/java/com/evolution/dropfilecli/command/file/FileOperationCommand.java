package com.evolution.dropfilecli.command.file;

import picocli.CommandLine;

@CommandLine.Command(
        name = "file",
        description = "File operation",
        subcommands = {
                FileOperationListCommand.class,
                FileOperationDownloadFile.class
        }
)
public class FileOperationCommand implements Runnable {

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(System.out);
    }
}

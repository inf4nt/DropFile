package com.evolution.dropfilecli.command;

import com.evolution.dropfilecli.command.connect.ConnectCommand;
import com.evolution.dropfilecli.command.daemon.DaemonCommand;
import com.evolution.dropfilecli.command.file.FileOperationCommand;
import com.evolution.dropfilecli.command.file.FileOperationDownloadFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        name = "app",
        mixinStandardHelpOptions = true,
        versionProvider = ManifestVersionProvider.class,
        subcommands = {
                ConnectCommand.class,
                FileOperationCommand.class,
                DaemonCommand.class
        }
)
public class RootCommand implements Runnable {

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Autowired
    private FileOperationDownloadFile fileOperationDownloadFile;

    @Override
    public void run() {
        spec.commandLine().usage(System.out);
//        fileOperationDownloadFile.run();
    }
}
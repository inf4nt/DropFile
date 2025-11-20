package com.evolution.dropfilecli;

import com.evolution.dropfilecli.command.ConnectionsCommand;
import com.evolution.dropfilecli.configuration.DropFileCliConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        mixinStandardHelpOptions = true,
        versionProvider = ManifestVersionProvider.class,
        subcommands = {
                ConnectionsCommand.class
        }
)
public class RootCommand implements Runnable {

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Autowired
    private DropFileCliConfiguration dropFileCliConfiguration;

    @Override
    public void run() {
        System.out.println("Daemon address: " + dropFileCliConfiguration.getDaemonAddress());
        System.out.println("Download directory: " + dropFileCliConfiguration.getDownloadDirectory());
        spec.commandLine().usage(System.out);
    }
}
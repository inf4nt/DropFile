package com.evolution.dropfilecli.command;

import com.evolution.dropfilecli.configuration.DropFileCliConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        name = "config",
        description = "Show configuration"
)
public class ConfigurationCommand implements Runnable {

    private final DropFileCliConfiguration dropFileCliConfiguration;

    @Autowired
    public ConfigurationCommand(DropFileCliConfiguration dropFileCliConfiguration) {
        this.dropFileCliConfiguration = dropFileCliConfiguration;
    }

    @Override
    public void run() {
        System.out.println(dropFileCliConfiguration);
    }
}

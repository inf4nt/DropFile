package com.evolution.dropfilecli.command;

import com.evolution.dropfilecli.configuration.DropFileConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        name = "configuration",
        description = "Show configuration"
)
public class ConfigurationCommand implements Runnable {

    private final DropFileConfiguration dropFileConfiguration;

    @Autowired
    public ConfigurationCommand(DropFileConfiguration dropFileConfiguration) {
        this.dropFileConfiguration = dropFileConfiguration;
    }

    @Override
    public void run() {
        System.out.println("Configuration " + dropFileConfiguration);
    }
}

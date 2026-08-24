package com.evolution.dropfilecli.command;

import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(
        name = "version",
        aliases = {"--version", "-v"},
        description = "Retrieve a project version",
        customSynopsis = "dropfile version",
        parameterListHeading = "%nRequired parameters:%n",
        optionListHeading = "%nOptional parameters:%n"
)
public class VersionCommand implements SimpleCommandHandler {

    @Override
    public void handle() throws Exception {
        Package pkg = this.getClass().getPackage();
        String version = pkg.getImplementationVersion();
        System.out.println(version);
    }
}

package com.evolution.dropfilecli.command;

import com.evolution.dropfilecli.util.Spinner;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
public class ManifestVersionProvider implements CommandLine.IVersionProvider {

    @Override
    public String[] getVersion() {
        Spinner.stop();

        Package pkg = this.getClass().getPackage();
        String version = pkg.getImplementationVersion();
        return new String[]{version == null ? "unknown" : version};
    }
}
